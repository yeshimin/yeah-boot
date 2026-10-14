package com.yeshimin.yeahboot.auth.common.config.security;

import com.yeshimin.yeahboot.api.encryption.ApiEncryptionQueryFilter;
import com.yeshimin.yeahboot.api.encryption.ApiEncryptionController;
import com.yeshimin.yeahboot.auth.service.AuthService;
import com.yeshimin.yeahboot.common.common.log.MdcLogFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    /** 提供登录认证与当前用户解析能力的认证服务。 */
    private final AuthService authService;

    /** 扫描 {@link PublicAccess} 并解析公开接口路径的 MVC 映射。 */
    private final RequestMappingHandlerMapping handlerMapping;

    /** 在 Security 路径授权后解密查询参数的过滤器。 */
    private final ApiEncryptionQueryFilter apiEncryptionQueryFilter;

    /** 是否开放 SpringDoc API 定义与 UI 路径。 */
    @Value("${springdoc.api-docs.enabled:true}")
    private boolean springdocApiDocsEnabled;

    /** 是否兼容从 URL 查询参数读取 Token。 */
    @Value("${auth.token.support-query-token:true}")
    private boolean supportQueryToken;

    /**
     * 创建安全配置并指定用于公开接口扫描的 MVC 映射。
     *
     * <p>引入 actuator 后，controllerEndpointHandlerMapping 会与 requestMappingHandlerMapping 同时存在，
     * 因此必须显式限定后者。</p>
     */
    public WebSecurityConfig(AuthService authService,
                             @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                             ApiEncryptionQueryFilter apiEncryptionQueryFilter) {
        this.authService = authService;
        this.handlerMapping = handlerMapping;
        this.apiEncryptionQueryFilter = apiEncryptionQueryFilter;
    }

    /**
     * 构建无状态 Security Filter 链。
     *
     * <p>与本项目安全能力相关的执行顺序为：MDC Filter → JWT 认证 Filter →
     * {@link AuthorizationFilter} 路径授权 → 查询参数加密 Filter。</p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager) throws Exception {
        Map<String, PublicAccess> publicAccessUrls = this.getPublicAccessUrls();
        // 公钥引导端点属于加密模块，不依赖 auth 模块的 @PublicAccess 注解。
        publicAccessUrls.put(ApiEncryptionController.PUBLIC_KEY_PATH, null);
        // SpringDoc 开启时才公开其 API 定义和界面路径。
        if (springdocApiDocsEnabled) {
            publicAccessUrls.putAll(this.getUrlsForSpringdoc());
        }
        log.info("Public access URLs: {}", publicAccessUrls);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                // API 不使用 HttpSession 保存认证状态。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(new JwtTokenAuthenticationFilter(
                        authenticationManager, publicAccessUrls, supportQueryToken), LogoutFilter.class)
                // 先完成路径授权，再在 MVC 解析 @RequestParam 前解密查询参数。
                .addFilterAfter(apiEncryptionQueryFilter, AuthorizationFilter.class)
                // MDC Filter 必须先于认证 Filter，使认证和后续日志都携带请求追踪信息。
                .addFilterBefore(new MdcLogFilter(), JwtTokenAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    if (!publicAccessUrls.isEmpty()) {
                        authorize.requestMatchers(publicAccessUrls.keySet().toArray(new String[0])).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                // 统一返回认证失败和权限不足响应。
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(Collections.singletonList(authenticationProvider));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new JwtTokenAuthenticationProvider(authService);
    }

    // ================================================================================

    private Map<String, PublicAccess> getPublicAccessUrls() {
//        Set<String> urls = new HashSet<>();
        Map<String, PublicAccess> urls = new HashMap<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            // 判断是否打了 @PublicAccess 且 enabled = true
            PublicAccess access = handlerMethod.getMethodAnnotation(PublicAccess.class);
            if (access != null && access.enabled()) {
                RequestMappingInfo mappingInfo = entry.getKey();

                // 获取该方法映射的 URL 路径
                Set<String> patterns = new HashSet<>();
                if (mappingInfo.getPathPatternsCondition() != null) {
                    patterns = mappingInfo.getPathPatternsCondition().getPatternValues();
                } else if (mappingInfo.getPatternsCondition() != null) {
                    patterns = mappingInfo.getPatternsCondition().getPatterns();
                } else {
                    log.warn("No patterns found for method: {}", handlerMethod.getMethod().getName());
                }
//                urls.addAll(patterns);
                for (String pattern : patterns) {
                    urls.put(pattern, access);
                }
            }
        }

        return urls;
    }

    /**
     * 获取静态资源路径 for springdoc
     */
    private Map<String, PublicAccess> getUrlsForSpringdoc() {
        Set<String> paths = new HashSet<>();
        paths.add("/v3/api-docs/**");
        paths.add("/**/*.html");
        paths.add("/**/*.js");
        paths.add("/**/*.css");

        Map<String, PublicAccess> urls0 = new HashMap<>();
        for (String path : paths) {
            urls0.put(path, null);
        }
        return urls0;
    }
}
