package com.yeshimin.yeahboot.app.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * App 独立启动时扫描公共数据层 Mapper，单入口模式由 Admin 入口统一扫描。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "yeah-boot.app-mapper-scan-enabled", havingValue = "true")
@MapperScan("com.yeshimin.yeahboot.**.mapper")
public class AppMybatisConfig {
}
