package com.yeshimin.yeahboot.auth.common.config.security;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Spring Security 的 JWT Token 认证过滤器
 */
@Slf4j
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private AuthenticationManager authenticationManager;
    private Map<String, PublicAccess> publicAccessUrls;
    private boolean supportQueryToken;

    public JwtTokenAuthenticationFilter(AuthenticationManager authenticationManager,
                                        Map<String, PublicAccess> publicAccessUrls,
                                        boolean supportQueryToken) {
        this.authenticationManager = authenticationManager;
        this.publicAccessUrls = publicAccessUrls;
        this.supportQueryToken = supportQueryToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("doFilterInternal()");

        // 按需认证并写入当前 Web 上下文
        authenticateIfRequired(request);

        filterChain.doFilter(request, response);

        // 请求结束后清理线程上下文
        WebContextUtils.clear();
    }

    /**
     * 根据接口公开策略和 Token 状态按需完成认证
     */
    private void authenticateIfRequired(HttpServletRequest request) {
        String token = this.extractToken(request);
        if (StringUtils.isBlank(token) || this.shouldSkipAuth(request.getRequestURI())) {
            log.debug("no token or public access, as anonymous in later AnonymousAuthenticationFilter");
            // 在过滤链AnonymousAuthenticationFilter中设置为匿名用户
            // 这里要先clear，否则在匿名Filter中会存在JwtTokenAuthenticationToken；具体原因暂时未知
            SecurityContextHolder.clearContext();
            return;
        }

        // 封装认证请求
        Authentication authRequest = new JwtTokenAuthenticationToken(token);
        // 委派 ProviderManager 执行认证
        Authentication authResult = authenticationManager.authenticate(authRequest);
        // 写入 Spring Security 上下文
        SecurityContextHolder.getContext().setAuthentication(authResult);
    }

    /**
     * 优先从 Authorization 请求头提取 Token；兼容模式下才读取查询参数
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(CommonConsts.TOKEN_HEADER_KEY);
        if (StringUtils.isBlank(authHeader)) {
            // 仅在显式启用兼容模式时尝试从query参数中获取
            return supportQueryToken ? this.getTokenFromParameter(request) : null;
        } else {
            return authHeader.replace("Bearer ", "");
        }
    }

    /**
     * 判断当前接口是否允许跳过认证
     */
    private boolean shouldSkipAuth(String url) {
        return this.isPublicAccessUrl(url) && !this.isAuthOnDemand(url);
    }

    /**
     * 判断 URL 是否标记为匿名访问
     */
    private boolean isPublicAccessUrl(String url) {
        boolean r = publicAccessUrls.containsKey(url);
        log.debug("isPublicAccessUrl: {}, url: {}", r, url);
        return r;
    }

    /**
     * 判断匿名接口是否允许携带 Token 进行按需认证
     */
    private boolean isAuthOnDemand(String url) {
        PublicAccess access = publicAccessUrls.get(url);
        boolean r = access != null && access.authOnDemand();
        log.debug("isAuthOnDemand: {}, url: {}", r, url);
        return r;
    }

    /**
     * 从兼容查询参数中按顺序读取 {@code access_token}、{@code accessToken}、{@code token}
     */
    private String getTokenFromParameter(HttpServletRequest request) {
        String token = request.getParameter("access_token");
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        token = request.getParameter("accessToken");
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        token = request.getParameter("token");
        return token;
    }
}
