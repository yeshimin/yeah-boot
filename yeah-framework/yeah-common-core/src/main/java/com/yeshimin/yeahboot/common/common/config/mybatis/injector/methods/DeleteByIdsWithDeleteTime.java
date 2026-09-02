package com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.methods.DeleteByIds;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;

import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * 按ID集合逻辑删除时自动记录删除时间
 */
public class DeleteByIdsWithDeleteTime extends DeleteByIds {

    /**
     * 生成按主键集合逻辑删除的SQL脚本
     * <p>
     * 物理删除分支继续由父类{@link DeleteByIds}处理，本方法只扩展逻辑删除分支。
     * </p>
     *
     * @param tableInfo 当前实体和数据库表的映射信息
     * @param sqlMethod MyBatis Plus批量逻辑删除SQL模板
     * @return 可由MyBatis解析执行的批量逻辑删除SQL
     */
    @Override
    public String logicDeleteScript(TableInfo tableInfo, SqlMethod sqlMethod) {
        // 查找需要在UPDATE时自动填充的字段，例如updateTime和updateBy
        List<TableFieldInfo> fieldInfos = tableInfo.getFieldList().stream()
                .filter(TableFieldInfo::isWithUpdateFill)
                // 逻辑删除字段deleted由框架单独生成，不能在这里重复处理
                .filter(field -> !field.isLogicDelete())
                .collect(toList());

        // 从SET关键字开始构建逻辑删除的字段更新片段
        String sqlSet = "SET ";
        if (CollectionUtils.isNotEmpty(fieldInfos)) {
            // 批量删除时MyBatis Plus会提供mpFillEt对象，用于填充updateTime、updateBy等字段
            sqlSet += SqlScriptUtils.convertIf(fieldInfos.stream()
                            .map(field -> field.getSqlSet(Constants.MP_FILL_ET + StringPool.DOT))
                            .collect(joining(EMPTY)),
                    String.format("%s != null", Constants.MP_FILL_ET), true);
        }

        // 追加MyBatis Plus原有的逻辑删除字段，例如“deleted=1”
        sqlSet += tableInfo.getLogicDeleteSql(false, false);

        // 在框架原有SET片段最前面追加“delete_time=CURRENT_TIMESTAMP”
        sqlSet = LogicDeleteSqlUtils.appendDeleteTime(tableInfo, sqlSet);

        // 将表名、SET片段、主键列、ID集合和未删除条件填入框架模板
        return String.format(sqlMethod.getSql(), tableInfo.getTableName(), sqlSet,
                tableInfo.getKeyColumn(), SqlScriptUtils.convertForeach(
                        // 集合元素既支持简单ID，也支持包含ID属性的实体对象
                        SqlScriptUtils.convertChoose(
                                "@org.apache.ibatis.type.SimpleTypeRegistry@isSimpleType(item.getClass())",
                                "#{item}", "#{item." + tableInfo.getKeyProperty() + "}"),
                        // 生成“item IN (#{item}, #{item}, ...)”对应的foreach脚本
                        COLL, null, "item", COMMA),
                // 只删除当前尚未被逻辑删除的数据，例如“deleted=0”
                tableInfo.getLogicDeleteSql(true, true));
    }
}
