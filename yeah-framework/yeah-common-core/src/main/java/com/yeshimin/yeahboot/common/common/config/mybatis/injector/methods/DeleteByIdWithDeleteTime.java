package com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.methods.DeleteById;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * 按ID逻辑删除时自动记录删除时间
 */
public class DeleteByIdWithDeleteTime extends DeleteById {

    /**
     * 为当前Mapper生成按单个主键删除的MappedStatement
     *
     * @param mapperClass 当前Mapper类型
     * @param modelClass  当前实体类型
     * @param tableInfo   当前实体和数据库表的映射信息
     * @return 注册到MyBatis中的删除语句定义
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 最终需要交给MyBatis解析的SQL脚本
        String sql;

        // 根据是否配置逻辑删除，在后续选择逻辑删除或物理删除模板
        SqlMethod sqlMethod;

        // 实体配置了@TableLogic时，生成按ID更新删除标识的SQL
        if (tableInfo.isWithLogicDelete()) {
            // MyBatis Plus内置的单ID逻辑删除模板
            sqlMethod = SqlMethod.LOGIC_DELETE_BY_ID;

            // 查找需要在UPDATE时自动填充的字段，例如updateTime和updateBy
            List<TableFieldInfo> fieldInfos = tableInfo.getFieldList().stream()
                    .filter(TableFieldInfo::isWithUpdateFill)
                    // 逻辑删除字段deleted由框架单独生成，不能在这里重复处理
                    .filter(field -> !field.isLogicDelete())
                    .collect(toList());

            // SQL的SET部分，例如“SET update_time=?,update_by=?,deleted=1”
            String sqlSet;
            if (CollectionUtils.isNotEmpty(fieldInfos)) {
                // 参数为实体对象时，保留MyBatis Plus原有的更新字段自动填充逻辑
                sqlSet = "SET " + SqlScriptUtils.convertIf(fieldInfos.stream()
                        .map(field -> field.getSqlSet(EMPTY)).collect(joining(EMPTY)),
                        "!@org.apache.ibatis.type.SimpleTypeRegistry@isSimpleType(_parameter.getClass())", true)
                        + tableInfo.getLogicDeleteSql(false, false);
            } else {
                // 没有更新填充字段时，只生成“SET deleted=1”
                sqlSet = sqlLogicSet(tableInfo);
            }

            // 在框架原有SET片段最前面追加“delete_time=CURRENT_TIMESTAMP”
            sqlSet = LogicDeleteSqlUtils.appendDeleteTime(tableInfo, sqlSet);

            // 将表名、SET片段、主键列、主键属性和未删除条件填入框架模板
            sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), sqlSet,
                    tableInfo.getKeyColumn(), tableInfo.getKeyProperty(), tableInfo.getLogicDeleteSql(true, true));

            // 将SQL字符串解析成MyBatis可以执行的SqlSource
            SqlSource sqlSource = super.createSqlSource(configuration, sql, Object.class);

            // 逻辑删除本质是UPDATE，因此注册为Update类型的MappedStatement
            return addUpdateMappedStatement(mapperClass, modelClass, methodName, sqlSource);
        }

        // 实体没有@TableLogic时，完全保留MyBatis Plus默认的按ID物理删除行为
        sqlMethod = SqlMethod.DELETE_BY_ID;
        sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(),
                tableInfo.getKeyColumn(), tableInfo.getKeyProperty());

        // 物理删除注册为Delete类型的MappedStatement
        return this.addDeleteMappedStatement(mapperClass, methodName,
                super.createSqlSource(configuration, sql, Object.class));
    }
}
