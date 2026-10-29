package com.yeshimin.yeahboot.auth.common.config.security;

import com.yeshimin.yeahboot.auth.common.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 将框架跨域配置注册到 Spring MVC
 */
@Configuration
@RequiredArgsConstructor
public class CorsWebMvcConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * 注册全局跨域规则，并与 Controller 上的 @CrossOrigin 共同生效
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsProperties.isEnabled()) {
            return;
        }

        CorsRegistration registration = registry.addMapping(corsProperties.getPathPattern());
        registration.allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(new String[0]));
        registration.allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]));
        registration.allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]));
        registration.exposedHeaders(corsProperties.getExposedHeaders().toArray(new String[0]));
        registration.allowCredentials(corsProperties.isAllowCredentials());
        registration.maxAge(corsProperties.getMaxAge());
    }
}
