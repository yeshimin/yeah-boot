#!/usr/bin/env python3
"""Convert common MySQL 8 dump syntax to MySQL 5.7-compatible SQL.

The converter keeps INSERT values unchanged. When generated columns are added,
it adds explicit physical column names to INSERT statements. It handles the
schema differences used by YeahBoot's dumps:

* MySQL 8 utf8mb4 collations are mapped to MySQL 5.7 equivalents.
* Functional indexes are converted to stored generated columns plus indexes.

Usage:
    python3 script/mysql8_to_mysql57.py \
      --input sql/00.yeah_boot_main.sql \
      --output sql/00.yeah_boot_main.mysql57.sql

For a data-only dump, provide the original structure dump with --schema so
INSERT statements can omit generated columns added during conversion:

    python3 script/mysql8_to_mysql57.py \
      --input yeahboot_data.sql \
      --schema yeahboot_structure.sql \
      --output yeahboot_data.mysql57.sql
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from dataclasses import dataclass
from pathlib import Path


COLLATION_MAP = {
    "utf8mb4_0900_ai_ci": "utf8mb4_unicode_ci",
    "utf8mb4_0900_as_ci": "utf8mb4_unicode_ci",
    "utf8mb4_0900_as_cs": "utf8mb4_bin",
    "utf8mb4_0900_bin": "utf8mb4_bin",
    "utf8mb4_uca1400_ai_ci": "utf8mb4_unicode_ci",
    "utf8mb4_uca1400_as_ci": "utf8mb4_unicode_ci",
    "utf8mb4_uca1400_as_cs": "utf8mb4_bin",
}

MYSQL8_COLLATION_PATTERN = re.compile(r"utf8mb4_(?:0900|uca1400)_[a-z0-9_]+", re.IGNORECASE)
CREATE_TABLE_PATTERN = re.compile(r"\bCREATE\s+TABLE\b", re.IGNORECASE)
CREATE_TABLE_NAME_PATTERN = re.compile(
    r"\bCREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?"
    r"(?P<table>(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)(?:\.(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*))?)",
    re.IGNORECASE,
)
FUNCTIONAL_INDEX_PATTERN = re.compile(
    r"^\s*(?P<kind>UNIQUE\s+KEY|KEY|INDEX)\s+"
    r"(?P<name>`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)\s*"
    r"(?P<body>\(.*\))\s*$",
    re.IGNORECASE | re.DOTALL,
)
KEY_DEFINITION_PATTERN = re.compile(
    r"^\s*(?:PRIMARY\s+KEY|UNIQUE\s+KEY|KEY|INDEX|CONSTRAINT|FOREIGN\s+KEY|CHECK)\b",
    re.IGNORECASE,
)
INSERT_VALUES_PATTERN = re.compile(
    r"(?im)(?P<prefix>^[ \t]*INSERT\s+(?:IGNORE\s+)?INTO\s+"
    r"(?P<table>(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)(?:\.(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*))?))"
    r"(?P<space>\s+)VALUES\b"
)


class CompatibilityError(ValueError):
    """Raised when a MySQL 8 feature cannot be converted safely."""


@dataclass
class ConversionStats:
    collations: int = 0
    functional_indexes: int = 0
    create_tables: int = 0
    insert_statements: int = 0


def find_matching_parenthesis(text: str, opening: int) -> int:
    """Return the closing parenthesis while honoring SQL string/backtick quotes."""
    depth = 0
    quote: str | None = None
    index = opening

    while index < len(text):
        char = text[index]
        if quote:
            if char == "\\" and quote in {"'", '"'}:
                index += 2
                continue
            if char == quote:
                if quote in {"'", '"'} and index + 1 < len(text) and text[index + 1] == quote:
                    index += 2
                    continue
                quote = None
            index += 1
            continue

        if char in {"'", '"', "`"}:
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return index
        index += 1

    raise CompatibilityError("无法找到 CREATE TABLE 的结束括号")


def split_top_level(value: str) -> list[str]:
    """Split a CREATE TABLE body by commas outside nested SQL expressions."""
    parts: list[str] = []
    start = 0
    depth = 0
    quote: str | None = None
    index = 0

    while index < len(value):
        char = value[index]
        if quote:
            if char == "\\" and quote in {"'", '"'}:
                index += 2
                continue
            if char == quote:
                if quote in {"'", '"'} and index + 1 < len(value) and value[index + 1] == quote:
                    index += 2
                    continue
                quote = None
            index += 1
            continue

        if char in {"'", '"', "`"}:
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        elif char == "," and depth == 0:
            parts.append(value[start:index].strip())
            start = index + 1
        index += 1

    final_part = value[start:].strip()
    if final_part:
        parts.append(final_part)
    return parts


def split_function_arguments(expression: str) -> list[str]:
    function_opening = expression.find("(")
    if function_opening < 0:
        raise CompatibilityError(f"无法解析函数索引表达式：{expression}")
    function_closing = find_matching_parenthesis(expression, function_opening)
    if expression[function_closing + 1 :].strip():
        raise CompatibilityError(f"函数索引表达式格式不受支持：{expression}")
    return split_top_level(expression[function_opening + 1 : function_closing])


def unwrap_parentheses(value: str) -> str:
    value = value.strip()
    while value.startswith("(") and value.endswith(")"):
        try:
            closing = find_matching_parenthesis(value, 0)
        except CompatibilityError:
            break
        if closing != len(value) - 1:
            break
        value = value[1:-1].strip()
    return value


def infer_target_column(expression: str) -> str:
    """Infer IF(condition, column, NULL)'s source column for generated indexes."""
    if not re.match(r"^\s*if\s*\(", expression, re.IGNORECASE):
        raise CompatibilityError(f"仅支持 IF 形式的函数索引：{expression}")

    arguments = split_function_arguments(expression)
    if len(arguments) != 3 or unwrap_parentheses(arguments[2]).upper() != "NULL":
        raise CompatibilityError(f"仅支持 IF(condition, column, NULL) 形式：{expression}")

    target = unwrap_parentheses(arguments[1])
    match = re.fullmatch(r"`([^`]+)`|([A-Za-z_][A-Za-z0-9_$]*)", target)
    if not match:
        raise CompatibilityError(f"无法推断函数索引的目标列：{expression}")
    return match.group(1) or match.group(2)


def parse_functional_index(definition: str) -> tuple[str, str, list[str]] | None:
    """Return a functional index definition, or None for a regular index."""
    match = FUNCTIONAL_INDEX_PATTERN.match(definition)
    if not match:
        return None

    body = match.group("body").strip()
    if find_matching_parenthesis(body, 0) != len(body) - 1:
        raise CompatibilityError(f"函数索引格式不受支持：{definition}")

    index_items = split_top_level(body[1:-1])
    if not index_items:
        raise CompatibilityError(f"函数索引缺少表达式：{definition}")

    expressions: list[str] = []
    for item in index_items:
        item = item.strip()
        if not item.startswith("(") or find_matching_parenthesis(item, 0) != len(item) - 1:
            # A regular key, such as KEY idx (`username`), is not converted.
            return None
        expressions.append(unwrap_parentheses(item))

    return match.group("kind"), match.group("name"), expressions


def find_column_type(definitions: list[str], column: str) -> str:
    pattern = re.compile(
        rf"^\s*`?{re.escape(column)}`?\s+"
        r"(?P<type>[A-Za-z]+(?:\s*\([^)]*\))?(?:\s+(?:UNSIGNED|SIGNED))?)(?=\s|$)",
        re.IGNORECASE,
    )
    for definition in definitions:
        match = pattern.match(definition)
        if match:
            return match.group("type")
    raise CompatibilityError(f"无法找到函数索引目标列 `{column}` 的类型")


def generated_column_name(index_name: str, existing_columns: set[str]) -> str:
    normalized = re.sub(r"[^A-Za-z0-9_]", "_", index_name.strip("`"))
    candidate = f"__m57_{normalized}"[:64]
    if candidate not in existing_columns:
        existing_columns.add(candidate)
        return candidate

    digest = hashlib.sha1(index_name.encode("utf-8")).hexdigest()[:10]
    candidate = f"__m57_{digest}"
    existing_columns.add(candidate)
    return candidate


def normalize_table_name(table: str) -> str:
    return table.split(".")[-1].strip("`").lower()


def get_table_name(statement: str) -> str:
    match = CREATE_TABLE_NAME_PATTERN.search(statement)
    if not match:
        raise CompatibilityError("无法解析 CREATE TABLE 的表名")
    return normalize_table_name(match.group("table"))


def get_physical_column_names(definitions: list[str]) -> list[str]:
    columns: list[str] = []
    for definition in definitions:
        if KEY_DEFINITION_PATTERN.match(definition):
            continue
        match = re.match(r"^\s*`([^`]+)`|^\s*([A-Za-z_][A-Za-z0-9_$]*)", definition)
        if not match or "GENERATED ALWAYS" in definition.upper():
            continue
        columns.append(match.group(1) or match.group(2))
    return columns


def transform_create_table(
    statement: str,
    stats: ConversionStats,
    insert_columns_by_table: dict[str, list[str]],
) -> str:
    table_match = CREATE_TABLE_PATTERN.search(statement)
    if not table_match:
        return statement

    opening = statement.find("(", table_match.end())
    if opening < 0:
        raise CompatibilityError("CREATE TABLE 缺少列定义")
    closing = find_matching_parenthesis(statement, opening)
    definitions = split_top_level(statement[opening + 1 : closing])
    existing_columns = {
        match.group(1) or match.group(2)
        for definition in definitions
        if (match := re.match(r"^\s*`([^`]+)`|^\s*([A-Za-z_][A-Za-z0-9_$]*)", definition))
        and not KEY_DEFINITION_PATTERN.match(definition)
    }

    generated_columns: list[str] = []
    converted_definitions: list[str] = []

    for definition in definitions:
        functional_index = parse_functional_index(definition)
        if functional_index is None:
            converted_definitions.append(definition)
            continue

        kind, index_name, expressions = functional_index
        generated_names: list[str] = []
        for index_position, expression in enumerate(expressions, start=1):
            target_column = infer_target_column(expression)
            column_type = find_column_type(definitions, target_column)
            generated_name = generated_column_name(
                f"{index_name.strip('`')}_{index_position}", existing_columns
            )
            generated_columns.append(
                f"`{generated_name}` {column_type} GENERATED ALWAYS AS ({expression}) STORED"
            )
            generated_names.append(f"`{generated_name}`")
        converted_definitions.append(
            f"{kind} {index_name} ({', '.join(generated_names)})"
        )
        stats.functional_indexes += 1

    if not generated_columns:
        return statement

    insert_columns_by_table[get_table_name(statement)] = get_physical_column_names(definitions)
    insert_at = next(
        (index for index, definition in enumerate(converted_definitions) if KEY_DEFINITION_PATTERN.match(definition)),
        len(converted_definitions),
    )
    converted_definitions[insert_at:insert_at] = generated_columns
    prefix = statement[:opening].rstrip()
    suffix = statement[closing + 1 :]
    formatted_definitions = ",\n  ".join(converted_definitions)
    stats.create_tables += 1
    return f"{prefix} (\n  {formatted_definitions}\n){suffix}"


def transform_create_tables(
    sql: str,
    stats: ConversionStats,
    insert_columns_by_table: dict[str, list[str]],
) -> str:
    output: list[str] = []
    cursor = 0

    while match := CREATE_TABLE_PATTERN.search(sql, cursor):
        output.append(sql[cursor : match.start()])
        opening = sql.find("(", match.end())
        if opening < 0:
            raise CompatibilityError("CREATE TABLE 缺少列定义")
        closing = find_matching_parenthesis(sql, opening)
        semicolon = sql.find(";", closing)
        if semicolon < 0:
            raise CompatibilityError("CREATE TABLE 缺少结束分号")
        output.append(
            transform_create_table(
                sql[match.start() : semicolon + 1], stats, insert_columns_by_table
            )
        )
        cursor = semicolon + 1

    output.append(sql[cursor:])
    return "".join(output)


def collect_insert_column_mapping(schema_sql: str) -> dict[str, list[str]]:
    """Find original physical columns for tables that have functional indexes."""
    mapping: dict[str, list[str]] = {}
    cursor = 0

    while match := CREATE_TABLE_PATTERN.search(schema_sql, cursor):
        opening = schema_sql.find("(", match.end())
        if opening < 0:
            raise CompatibilityError("CREATE TABLE 缺少列定义")
        closing = find_matching_parenthesis(schema_sql, opening)
        semicolon = schema_sql.find(";", closing)
        if semicolon < 0:
            raise CompatibilityError("CREATE TABLE 缺少结束分号")

        statement = schema_sql[match.start() : semicolon + 1]
        definitions = split_top_level(statement[opening - match.start() + 1 : closing - match.start()])
        if any(parse_functional_index(definition) is not None for definition in definitions):
            mapping[get_table_name(statement)] = get_physical_column_names(definitions)
        cursor = semicolon + 1

    return mapping


def rewrite_insert_values(
    sql: str, insert_columns_by_table: dict[str, list[str]], stats: ConversionStats
) -> str:
    """Add explicit physical column lists to INSERT ... VALUES statements."""
    if not insert_columns_by_table:
        return sql

    def replace(match: re.Match[str]) -> str:
        columns = insert_columns_by_table.get(normalize_table_name(match.group("table")))
        if not columns:
            return match.group(0)
        stats.insert_statements += 1
        column_list = ", ".join(f"`{column}`" for column in columns)
        return f"{match.group('prefix')} ({column_list}){match.group('space')}VALUES"

    return INSERT_VALUES_PATTERN.sub(replace, sql)


def replace_collations(sql: str, stats: ConversionStats) -> str:
    def replace(match: re.Match[str]) -> str:
        source = match.group(0).lower()
        target = COLLATION_MAP.get(source)
        if target is None:
            raise CompatibilityError(f"未支持的 MySQL 8 排序规则：{source}")
        stats.collations += 1
        return target

    return MYSQL8_COLLATION_PATTERN.sub(replace, sql)


def convert(sql: str, schema_sql: str | None = None) -> tuple[str, ConversionStats]:
    stats = ConversionStats()
    insert_columns_by_table = (
        collect_insert_column_mapping(schema_sql) if schema_sql is not None else {}
    )
    sql = replace_collations(sql, stats)
    converted_mappings: dict[str, list[str]] = {}
    sql = transform_create_tables(sql, stats, converted_mappings)
    insert_columns_by_table.update(converted_mappings)
    sql = rewrite_insert_values(sql, insert_columns_by_table, stats)
    return sql, stats


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path, help="MySQL 8 导出的 SQL 文件")
    parser.add_argument(
        "--schema",
        type=Path,
        help="数据 Dump 单独转换时使用的原始 MySQL 8 结构 SQL 文件",
    )
    parser.add_argument("--output", required=True, type=Path, help="MySQL 5.7 兼容 SQL 输出文件")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.input.is_file():
        raise CompatibilityError(f"输入文件不存在：{args.input}")
    if args.schema is not None and not args.schema.is_file():
        raise CompatibilityError(f"结构文件不存在：{args.schema}")
    if args.input.resolve() == args.output.resolve():
        raise CompatibilityError("输出文件不能覆盖输入文件")

    sql = args.input.read_text(encoding="utf-8")
    schema_sql = args.schema.read_text(encoding="utf-8") if args.schema else None
    converted, stats = convert(sql, schema_sql)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(converted, encoding="utf-8")
    print(
        "转换完成："
        f"排序规则替换 {stats.collations} 处，"
        f"函数索引转换 {stats.functional_indexes} 个，"
        f"改写建表语句 {stats.create_tables} 个，"
        f"INSERT 补充列名 {stats.insert_statements} 条。"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except CompatibilityError as error:
        print(f"转换失败：{error}", file=sys.stderr)
        raise SystemExit(1)
