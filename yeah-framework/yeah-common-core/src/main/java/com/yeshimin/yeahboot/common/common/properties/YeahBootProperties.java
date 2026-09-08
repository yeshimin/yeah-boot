package com.yeshimin.yeahboot.common.common.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot")
public class YeahBootProperties {

    @PostConstruct
    public void init() {
        log.info("init [yeah-boot] properties...safeMode: {}, superAdmin: {}", safeMode, "******");
    }

    /**
     * 是否安全模式
     */
    private Boolean safeMode;

    /**
     * 超级管理员账号
     */
    private String superAdmin;
}
