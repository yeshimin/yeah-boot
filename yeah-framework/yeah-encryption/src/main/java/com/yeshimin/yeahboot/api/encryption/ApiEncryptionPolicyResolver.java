package com.yeshimin.yeahboot.api.encryption;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * 优先根据显式注解解析接口策略，其次匹配简洁 YAML 明文豁免规则。
 */
@Component
@RequiredArgsConstructor
public class ApiEncryptionPolicyResolver {

    /** 提供全局模式和 YAML 明文豁免规则的部署配置。 */
    private final ApiEncryptionProperties properties;

    /** 匹配 YAML 配置路径规则的 Ant 风格路径匹配器。 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 在应用启动时校验 YAML 明文豁免规则的格式，尽早阻断宽泛或非法配置。
     */
    @PostConstruct
    private void validateBypassRoutes() {
        for (String route : properties.getBypassRoutes()) {
            this.parseRoute(route);
        }
    }

    /**
     * 按方法注解、类注解、YAML 豁免和全局模式的优先级解析当前接口策略。
     */
    public ApiEncryptionPolicy resolve(HttpServletRequest request, HandlerMethod handlerMethod) {
        if (!properties.isEnabled()) {
            return ApiEncryptionPolicy.BYPASS;
        }

        // 方法级显式策略优先于控制器级策略。
        ApiEncryption annotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), ApiEncryption.class);
        if (annotation == null || annotation.value() == ApiEncryptionPolicy.DEFAULT) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), ApiEncryption.class);
        }
        if (annotation != null && annotation.value() != ApiEncryptionPolicy.DEFAULT) {
            return annotation.value();
        }

        // 没有显式注解时，再匹配部署配置的明文豁免规则。
        if (isBypassRoute(request)) {
            return ApiEncryptionPolicy.BYPASS;
        }
        return properties.getMode() == ApiEncryptionMode.REQUIRED
                ? ApiEncryptionPolicy.REQUIRED : ApiEncryptionPolicy.DEFAULT;
    }

    /**
     * 判断请求是否命中 YAML 配置的精确方法和路径豁免。
     */
    private boolean isBypassRoute(HttpServletRequest request) {
        for (String route : properties.getBypassRoutes()) {
            RouteSpec spec = this.parseRoute(route);
            if (spec == null) {
                continue;
            }
            if (spec.method != null && !spec.method.matches(request.getMethod())) {
                continue;
            }
            if (pathMatcher.match(spec.path, request.getRequestURI())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析“路径 [HTTP方法]”格式的豁免规则；未填写方法时匹配全部方法。
     */
    private RouteSpec parseRoute(String route) {
        if (!StringUtils.hasText(route)) {
            return null;
        }
        String[] parts = route.trim().split("\\s+");
        if (parts.length > 2 || !parts[0].startsWith("/") || parts[0].contains("**")) {
            throw new IllegalStateException("invalid api.encryption.bypass-routes entry: " + route);
        }
        if (parts.length == 1) {
            return new RouteSpec(parts[0], null);
        }
        try {
            return new RouteSpec(parts[0], HttpMethod.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("invalid HTTP method in payload encryption bypass route: " + route, e);
        }
    }

    /**
     * 解析后的接口豁免规则。
     */
    private static final class RouteSpec {

        /** Ant 风格的请求路径规则。 */
        private final String path;

        /** 可选 HTTP 方法；为空时匹配全部方法。 */
        private final HttpMethod method;

        /**
         * 创建路径与可选 HTTP 方法组成的豁免规则。
         *
         * @param path   路径规则
         * @param method 可选 HTTP 方法
         */
        private RouteSpec(String path, HttpMethod method) {
            this.path = path;
            this.method = method;
        }
    }
}
