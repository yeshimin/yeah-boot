package com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

/**
 * 逻辑删除SQL工具
 */
final class LogicDeleteSqlUtils {

    /**
     * BaseEntity中删除时间对应的Java属性名
     */
    private static final String DELETE_TIME_PROPERTY = "deleteTime";

    /**
     * 使用数据库时间，避免应用服务器和数据库服务器时间不一致
     */
    private static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    /**
     * 工具类禁止实例化
     */
    private LogicDeleteSqlUtils() {
    }

    /**
     * 在逻辑删除SET片段中追加删除时间
     * <p>
     * 处理前：{@code SET deleted=1}
     * </p>
     * <p>
     * 处理后：{@code SET delete_time=CURRENT_TIMESTAMP,deleted=1}
     * </p>
     *
     * @param tableInfo 当前实体和数据库表的映射信息
     * @param sqlSet    MyBatis Plus生成的原始SET片段
     * @return 追加删除时间后的SET片段
     */
    static String appendDeleteTime(TableInfo tableInfo, String sqlSet) {
        // 根据Java属性deleteTime查找实际数据库列名，避免直接硬编码delete_time
        String column = tableInfo.getFieldList().stream()
                .filter(field -> DELETE_TIME_PROPERTY.equals(field.getProperty()))
                .map(TableFieldInfo::getColumn)
                .findFirst()
                .orElse(null);

        // 当前实体没有deleteTime字段，或传入的不是标准SET片段时，保持框架原始SQL
        if (column == null || !sqlSet.startsWith("SET ")) {
            return sqlSet;
        }

        // 去掉原字符串开头的“SET ”，在最前面插入删除时间后再拼回原SET内容
        return "SET " + column + "=" + CURRENT_TIMESTAMP + "," + sqlSet.substring(4);
    }
}
