package com.yeshimin.yeahboot.admin.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

/**
 * 管理端登录限制配置
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.admin-login")
public class AdminLoginProperties {

    /**
     * 登录失败次数统计窗口，单位：秒
     */
    @Min(value = 1, message = "登录失败统计窗口必须大于0")
    private Long failureWindowSeconds = 10 * 60L;

    /**
     * 触发临时锁定的最大登录失败次数
     */
    @Min(value = 1, message = "最大登录失败次数必须大于0")
    private Long maxFailureCount = 5L;

    /**
     * 临时锁定时长，单位：秒
     */
    @Min(value = 1, message = "登录锁定时长必须大于0")
    private Long lockSeconds = 10 * 60L;
}
