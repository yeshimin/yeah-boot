package com.yeshimin.yeahboot.common.common.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yeshimin.yeahboot.common.common.config.mybatis.injector.YeahBootSqlInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatisPlus相关配置
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 扩展MyBatis Plus默认删除SQL，逻辑删除时自动记录删除时间
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new YeahBootSqlInjector();
    }

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
