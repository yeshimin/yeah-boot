package com.yeshimin.yeahboot.common.common.config.mybatis;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.domain.base.BaseQueryDto;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class QueryHelper<T> {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // yyyy-MM-dd
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");
    // yyyy-MM-dd HH:mm:ss
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-" +    // 年-月
                    "(0[1-9]|[12]\\d|3[01]) " +                   // 日
                    "([01]\\d|2[0-3]):" +                         // 时（00~23）
                    "[0-5]\\d:" +                                 // 分（00~59）
                    "[0-5]\\d$");                                 // 秒（00~59）

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    /**
     * 生成QueryWrapper
     * 只按查询类定义进行查询
     */
    public static <T> LambdaQueryWrapper<T> getQueryWrapper(Object query) {
        return getQueryWrapper(query, Wrappers.query());
    }

    /**
     * 生成QueryWrapper
     *
     * @param clazz 实体类Class，用于设置按实体类查询
     */
    public static <T> LambdaQueryWrapper<T> getQueryWrapper(Object query, Class<T> clazz) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (clazz.isInstance(query)) {
            wrapper.setEntity(clazz.cast(query));
        }
        return getQueryWrapper(query, wrapper);
    }

    /**
     * 暂时不开放外部调用，后续有需要的场景再开放
     */
    private static <T> LambdaQueryWrapper<T> getQueryWrapper(Object query, QueryWrapper<T> wrapper) {
        // 获取@Query注解
        Query queryAnno = query.getClass().getAnnotation(Query.class);
        // 是否启用查询
        boolean queryEnabled = queryAnno != null && queryAnno.enabled();
        // 是否启用自定义查询
        boolean queryCustom = queryAnno != null && queryAnno.custom();
        if (!queryEnabled) {
            log.debug("class: [{}], @Query is not enabled, skip", query.getClass().getName());
            return wrapper.lambda();
        }

        // 解析自定义查询条件
        List<Condition> listCondition = parseConditions(queryCustom, query);

        // 解析类字段定义的查询条件
        Field[] fields = ReflectUtil.getFields(query.getClass());
        for (Field field : fields) {
            // 跳过conditions字段
            if (field.getName().equals(CommonConsts.CONDITIONS_FIELD_NAME)) {
                log.debug("skip 'conditions' field");
                continue;
            }

            // 判断是否有 @QueryField 注解
            QueryField queryField = field.getAnnotation(QueryField.class);
            if (queryField == null) {
                log.debug("{} is not annotated with @QueryField, skip", field.getName());
                continue;
            }

            // 获取原访问标识，用于复原
            boolean isAccessible = field.isAccessible();
            // 允许访问私有字段
            field.setAccessible(true);
            try {
                Object value = field.get(query);

                // 获取条件生效策略
                QueryField.ConditionStrategy conditionStrategy = queryField.conditionStrategy();
                if (conditionStrategy == QueryField.ConditionStrategy.NOT_BLANK) {
                    if (value == null) {
                        log.debug("{} is null, skip", field.getName());
                        continue;
                    }
                    if (value instanceof String && StrUtil.isBlank((String) value)) {
                        log.debug("{} is blank, skip", field.getName());
                        continue;
                    }
                } else if (conditionStrategy == QueryField.ConditionStrategy.NOT_NULL) {
                    if (value == null) {
                        log.debug("{} is null, skip", field.getName());
                        continue;
                    }
                } else {
                    log.warn("conditionStrategy is not supported, skip");
                    continue;
                }

                // 获取查询类型
                QueryField.Type type = queryField.value() == QueryField.Type.DEFAULT ?
                        (queryField.type() == QueryField.Type.DEFAULT ? QueryField.Type.EQ : queryField.type()) :
                        queryField.value();

                listCondition.add(new Condition(field.getName(), type, value));
            } catch (IllegalAccessException e) {
                log.error("读取查询字段失败: {}", field.getName(), e);
            } finally {
                field.setAccessible(isAccessible);
            }
        }

        for (Condition condition : listCondition) {
            String fieldName = condition.getProperty();
            // 客户端传入的字段名必须是当前查询类的真实属性，避免任意字段名进入SQL
            Field field = ReflectUtil.getField(query.getClass(), fieldName);
            if (field == null
                    || Modifier.isStatic(field.getModifiers())
                    || field.isSynthetic()
                    || CommonConsts.CONDITIONS_FIELD_NAME.equals(field.getName())) {
                throw new BaseException("不支持的查询字段: " + fieldName);
            }
            // 实体类字段命名转为表字段命名（小驼峰转下划线）
            String columnName = StrUtil.toUnderlineCase(field.getName());
            QueryField.Type operator = condition.getOperator();
            Object value = condition.getValue();

            switch (operator) {
                // equal
                case EQ:
                    wrapper.eq(columnName, value);
                    break;
                // not equal
                case NE:
                    wrapper.ne(columnName, value);
                    break;
                // greater than
                case GT:
                    wrapper.gt(columnName, value);
                    break;
                // greater and equal
                case GE:
                    wrapper.ge(columnName, value);
                    break;
                // less than
                case LT:
                    wrapper.lt(columnName, value);
                    break;
                // less and equal
                case LE:
                    wrapper.le(columnName, value);
                    break;
                // in
                case IN:
                    if (value instanceof Object[]) {
                        if (((Object[]) value).length > 0) {
                            wrapper.in(columnName, (Object[]) value);
                        } else {
                            log.warn("{} is empty, skip", fieldName);
                        }
                    } else if (value instanceof Collection) {
                        if (!((Collection<?>) value).isEmpty()) {
                            wrapper.in(columnName, (Collection<?>) value);
                        } else {
                            log.warn("{} is empty, skip", fieldName);
                        }
                    } else if (value instanceof String) {
                        if (StrUtil.isNotBlank((String) value)) {
                            String[] arr = ((String) value).split(",");
                            if (arr.length > 0) {
                                wrapper.in(columnName, (Object[]) arr);
                            } else {
                                log.warn("{} is empty, skip", fieldName);
                            }
                        } else {
                            log.warn("{} is empty, skip", fieldName);
                        }
                    }
                    break;
                // not in
                case NOT_IN:
                    if (value instanceof Object[]) {
                        if (((Object[]) value).length > 0) {
                            wrapper.notIn(columnName, (Object[]) value);
                        } else {
                            log.warn("{} is empty, skip", fieldName);
                        }
                    } else if (value instanceof Collection) {
                        if (!((Collection<?>) value).isEmpty()) {
                            wrapper.notIn(columnName, (Collection<?>) value);
                        } else {
                            log.warn("{} is empty, skip", fieldName);
                        }
                    } else if (value instanceof String) {
                        if (StrUtil.isNotBlank((String) value)) {
                            String[] arr = ((String) value).split(",");
                            if (arr.length > 0) {
                                wrapper.notIn(columnName, (Object[]) arr);
                            } else {
                                log.warn("{} is empty, skip", fieldName);
                            }
                        } else {
                            log.warn("{} is empty, skip", fieldName);
                        }
                    }
                    break;
                // is blank or is empty
                case IS_BLANK:
                case IS_EMPTY:
                    wrapper.eq(columnName, "");
                    break;
                // is null
                case IS_NULL:
                    wrapper.isNull(columnName);
                    break;
                // is not null
                case IS_NOT_NULL:
                    wrapper.isNotNull(columnName);
                    break;
                // between
                case BETWEEN:
                    if (value instanceof Object[]) {
                        Object[] arr = (Object[]) value;
                        if (arr.length == 2) {
                            wrapper.between(columnName, arr[0], arr[1]);
                        } else {
                            log.warn("{} is invalid, skip", fieldName);
                        }
                    } else if (value instanceof Collection) {
                        Collection<?> col = (Collection<?>) value;
                        if (col.size() == 2) {
                            Iterator<?> iterator = col.iterator();
                            wrapper.between(columnName, iterator.next(), iterator.next());
                        } else {
                            log.warn("{} is invalid, skip", fieldName);
                        }
                    } else if (value instanceof String) {
                        // 日期字符串数组：2025-04-10,2025-04-13 | 2025-04-10 09:11:22,2025-04-13 14:13:33
                        String[] arr = ((String) value).split(",");
                        if (arr.length == 2) {
                            String begin = null;
                            String end = null;
                            if (DATE_PATTERN.matcher(arr[0]).matches()) {
                                begin = arr[0] + " 00:00:00";
                            } else if (DATE_TIME_PATTERN.matcher(arr[0]).matches()) {
                                begin = arr[0];
                            }
                            if (DATE_PATTERN.matcher(arr[1]).matches()) {
                                end = arr[1] + " 23:59:59";
                            } else if (DATE_TIME_PATTERN.matcher(arr[1]).matches()) {
                                end = arr[1];
                            }
                            if (begin != null && end != null) {
                                wrapper.between(columnName, begin, end);
                            } else {
                                log.warn("{} is invalid, skip", fieldName);
                            }
                        } else {
                            log.warn("{} is invalid, skip", fieldName);
                        }
                    } else {
                        log.warn("{} is invalid, skip", fieldName);
                    }
                    break;
                // not between
                case NOT_BETWEEN:
                    if (value instanceof Object[]) {
                        Object[] arr = (Object[]) value;
                        if (arr.length == 2) {
                            wrapper.notBetween(columnName, arr[0], arr[1]);
                        } else {
                            log.warn("{} is invalid, skip", fieldName);
                        }
                    } else if (value instanceof Collection) {
                        Collection<?> col = (Collection<?>) value;
                        if (col.size() == 2) {
                            Iterator<?> iterator = col.iterator();
                            wrapper.notBetween(columnName, iterator.next(), iterator.next());
                        } else {
                            log.warn("{} is invalid, skip", fieldName);
                        }
                    } else if (value instanceof String) {
                        // 日期字符串数组：2025-04-10,2025-04-13 | 2025-04-10 09:11:22,2025-04-13 14:13:33
                        String[] arr = ((String) value).split(",");
                        if (arr.length == 2) {
                            String begin = null;
                            String end = null;
                            if (DATE_PATTERN.matcher(arr[0]).matches()) {
                                begin = arr[0] + " 00:00:00";
                            } else if (DATE_TIME_PATTERN.matcher(arr[0]).matches()) {
                                begin = arr[0];
                            }
                            if (DATE_PATTERN.matcher(arr[1]).matches()) {
                                end = arr[1] + " 23:59:59";
                            } else if (DATE_TIME_PATTERN.matcher(arr[1]).matches()) {
                                end = arr[1];
                            }
                            if (begin != null && end != null) {
                                wrapper.notBetween(columnName, begin, end);
                            } else {
                                log.warn("{} is invalid, skip", fieldName);
                            }
                        } else {
                            log.warn("{} is invalid, skip", fieldName);
                        }
                    } else {
                        log.warn("{} is invalid, skip", fieldName);
                    }
                    break;
                // like
                case LIKE:
                    wrapper.like(columnName, value);
                    break;
                // like left
                case LIKE_LEFT:
                    wrapper.likeLeft(columnName, value);
                    break;
                // like right
                case LIKE_RIGHT:
                    wrapper.likeRight(columnName, value);
                    break;
                // not like
                case NOT_LIKE:
                    wrapper.notLike(columnName, value);
                    break;
                // not like left
                case NOT_LIKE_LEFT:
                    wrapper.notLikeLeft(columnName, value);
                    break;
                // not like right
                case NOT_LIKE_RIGHT:
                    wrapper.notLikeRight(columnName, value);
                    break;
                // sort
                case SORT:
                    if ("asc".equalsIgnoreCase(value.toString())) {
                        wrapper.orderByAsc(columnName);
                    } else if ("desc".equalsIgnoreCase(value.toString())) {
                        wrapper.orderByDesc(columnName);
                    } else {
                        log.warn("{} is invalid, skip", fieldName);
                    }
                    break;
                default:
                    break;
            }
        }

        return wrapper.lambda();
    }

    private static List<Condition> parseConditions(boolean queryCustom, Object query) {
        String conditions = null;
        if (queryCustom) {
            if (query instanceof BaseQueryDto) {
                conditions = ((BaseQueryDto) query).getConditions_();
            } else if (query instanceof ConditionBaseEntity) {
                conditions = ((ConditionBaseEntity<?>) query).getConditions_();
            } else {
                log.warn("query is not extends BaseQueryDto or ConditionBaseEntity, ignore");
            }
        }
        return conditions == null ? new ArrayList<>() : parseConditions(conditions);
    }

    /**
     * 解析自定义查询条件
     *
     * @param conditions 自定义查询条件（可能多个） 示例：username:likeLeft:yeshi;password:eq:123456
     * @return List<Condition>
     */
    private static List<Condition> parseConditions(String conditions) {
        List<Condition> list = new ArrayList<>();
        if (StrUtil.isBlank(conditions)) {
            log.debug("conditions is blank, ignore");
            return list;
        }
        String[] arr = conditions.split(";");
        for (String s : arr) {
            if (StrUtil.isBlank(s)) {
                log.warn("condition [{}] is blank, ignore", s);
                continue;
            }
            String[] arr2 = s.split(":", 3);
            // if length 2, padding to 3 with ""
            if (arr2.length == 2) {
                arr2 = new String[]{arr2[0], arr2[1], ""};
            }
            if (arr2.length != 3) {
                log.warn("condition [{}] is invalid, ignore", s);
                continue;
            }
            if (StrUtil.isBlank(arr2[0])) {
                log.warn("condition [{}] -> fieldName is blank, ignore", s);
                continue;
            }
            QueryField.Type operator = QueryField.Type.of(arr2[1]);
            if (operator == null) {
                log.warn("condition [{}] -> operator is invalid, ignore", s);
                continue;
            }

            list.add(new Condition(arr2[0], operator, arr2[2]));
        }
        return list;
    }

    @Data
    @AllArgsConstructor
    private static class Condition {
        private String property;
        QueryField.Type operator;
        Object value;
    }
}
