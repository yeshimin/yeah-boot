package com.yeshimin.yeahboot.auth.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 跨域访问配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.cors")
public class CorsProperties {

    /** 是否启用跨域访问 */
    private boolean enabled = false;

    /** 应用跨域规则的路径 */
    private String pathPattern = "/**";

    /** 允许的来源匹配规则 */
    private List<String> allowedOriginPatterns = new ArrayList<>();

    /** 允许的请求方法 */
    private List<String> allowedMethods = new ArrayList<>(
            Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    /** 允许的请求头 */
    private List<String> allowedHeaders = new ArrayList<>(Collections.singletonList("*"));

    /** 允许浏览器读取的响应头 */
    private List<String> exposedHeaders = new ArrayList<>(
            Arrays.asList("Content-Disposition", "Content-Length"));

    /** 是否允许浏览器携带 Cookie 等凭证 */
    private boolean allowCredentials = false;

    /** 预检请求缓存时间，单位：秒 */
    private long maxAge = 3600L;

    /**
     * 校验启用后的跨域规则
     */
    @PostConstruct
    private void validate() {
        if (!enabled) {
            return;
        }

        if (!StringUtils.hasText(pathPattern)) {
            throw new IllegalStateException("yeah-boot.cors.path-pattern 不能为空");
        }
        validateRequiredList(allowedOriginPatterns, "allowed-origin-patterns");
        validateRequiredList(allowedMethods, "allowed-methods");
        validateRequiredList(allowedHeaders, "allowed-headers");
        if (maxAge < 0) {
            throw new IllegalStateException("yeah-boot.cors.max-age 不能小于 0");
        }
    }

    private void validateRequiredList(List<String> values, String propertyName) {
        if (CollectionUtils.isEmpty(values)
                || values.stream().anyMatch(value -> !StringUtils.hasText(value))) {
            throw new IllegalStateException("yeah-boot.cors." + propertyName + " 不能为空");
        }
    }
}
