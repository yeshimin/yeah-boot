package com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.methods.Delete;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 条件逻辑删除时自动记录删除时间
 */
public class DeleteWithDeleteTime extends Delete {

    /**
     * 为当前Mapper生成按Wrapper条件删除的MappedStatement
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

        // LOGIC_DELETE是MyBatis Plus内置的条件逻辑删除SQL模板
        SqlMethod sqlMethod = SqlMethod.LOGIC_DELETE;

        // 实体配置了@TableLogic时，执行UPDATE逻辑删除，而不是物理DELETE
        if (tableInfo.isWithLogicDelete()) {
            // 获取框架默认SET片段“SET deleted=1”，并在其中追加删除时间
            String sqlSet = LogicDeleteSqlUtils.appendDeleteTime(tableInfo, sqlLogicSet(tableInfo));

            // 将表名、SET片段、Wrapper查询条件和SQL注释填入框架模板
            sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), sqlSet,
                    sqlWhereEntityWrapper(true, tableInfo), sqlComment());

            // 将SQL字符串解析成MyBatis可以执行的SqlSource
            SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);

            // 逻辑删除本质是UPDATE，因此注册为Update类型的MappedStatement
            return addUpdateMappedStatement(mapperClass, modelClass, methodName, sqlSource);
        }

        // 实体没有@TableLogic时，完全保留MyBatis Plus默认的物理删除行为
        sqlMethod = SqlMethod.DELETE;
        sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(),
                sqlWhereEntityWrapper(true, tableInfo), sqlComment());
        SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);

        // 物理删除注册为Delete类型的MappedStatement
        return this.addDeleteMappedStatement(mapperClass, methodName, sqlSource);
    }
}
