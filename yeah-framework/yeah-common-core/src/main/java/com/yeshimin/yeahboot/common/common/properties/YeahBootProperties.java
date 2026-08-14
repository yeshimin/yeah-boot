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
        log.info("init [yeah-boot] properties...captchaEnabled: {}, safeMode: {}, superAdmin: {}, smsCodeLength: {}",
                captchaEnabled, safeMode, superAdmin, smsCodeLength);
    }

    /**
     * 是否开启验证码校验
     */
    private Boolean captchaEnabled;

    /**
     * 是否安全模式
     */
    private Boolean safeMode;

    /**
     * 超级管理员账号
     */
    private String superAdmin;

    /**
     * 短信验证码长度
     */
    private Integer smsCodeLength;

    /**
     * 短信过期时间（秒）
     */
    private Integer smsCodeExpSeconds;
}
