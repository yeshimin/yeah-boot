package com.yeshimin.yeahboot.api.encryption;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 API 报文加密的 MVC 与 Servlet 集成。
 *
 * <p>查询参数过滤器由 Spring Security 链在路径授权后执行；MVC 限流拦截器使用顺序 {@code 0}，
 * 本类注册的加密策略拦截器使用顺序 {@code 10}。</p>
 */
@Configuration
@RequiredArgsConstructor
public class ApiEncryptionWebMvcConfig implements WebMvcConfigurer {

    /** 在 HandlerMethod 已解析后判定当前接口报文策略的拦截器。 */
    private final ApiEncryptionPolicyInterceptor apiEncryptionPolicyInterceptor;

    /**
     * 注册策略拦截器，使其在请求体转换前完成协议要求校验。
     *
     * <p>顺序必须晚于限流拦截器，避免超限请求进入加密报文处理链。</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiEncryptionPolicyInterceptor).addPathPatterns("/**").order(10);
    }

    /**
     * 禁用查询参数过滤器的外层 Servlet 自动注册。
     *
     * <p>该 Bean 仍由 Spring Security 在 {@code AuthorizationFilter} 后显式插入，避免同一个过滤器
     * 被容器链和安全链重复执行。</p>
     */
    @Bean
    public FilterRegistrationBean<ApiEncryptionQueryFilter> apiEncryptionQueryFilterRegistration(
            ApiEncryptionQueryFilter filter) {
        FilterRegistrationBean<ApiEncryptionQueryFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
