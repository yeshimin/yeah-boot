package com.yeshimin.yeahboot.auth.common.config.security;

import com.yeshimin.yeahboot.auth.service.AuthService;
import com.yeshimin.yeahboot.common.common.log.MdcLogFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final AuthService authService;

    private final RequestMappingHandlerMapping handlerMapping;

    // resolve: 引入actuator后，其中的'controllerEndpointHandlerMapping'会和'requestMappingHandlerMapping'冲突
    public WebSecurityConfig(AuthService authService,
                             @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.authService = authService;
        this.handlerMapping = handlerMapping;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager) throws Exception {
        Map<String, PublicAccess> publicAccessUrls = this.getPublicAccessUrls();
        // for springdoc
        publicAccessUrls.putAll(this.getUrlsForSpringdoc());
        log.info("Public access URLs: {}", publicAccessUrls);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                // 不需要session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(new JwtTokenAuthenticationFilter(authenticationManager, publicAccessUrls), LogoutFilter.class)
                // mdc filter设置到认证filter之前，使相关日志尽早附带mdc信息
                .addFilterBefore(new MdcLogFilter(), JwtTokenAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    if (!publicAccessUrls.isEmpty()) {
                        authorize.requestMatchers(publicAccessUrls.keySet().toArray(new String[0])).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                // 自定义认证失败处理
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
