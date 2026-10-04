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
 * Jwt Token认证过滤器 for Spring Security
 */
@Slf4j
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private AuthenticationManager authenticationManager;
    private Map<String, PublicAccess> publicAccessUrls;

    public JwtTokenAuthenticationFilter(AuthenticationManager authenticationManager,
                                        Map<String, PublicAccess> publicAccessUrls) {
        this.authenticationManager = authenticationManager;
        this.publicAccessUrls = publicAccessUrls;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("doFilterInternal()");

        // If required, authenticate and set web context
        authenticateIfRequired(request);

        filterChain.doFilter(request, response);

        // Clear web context
        WebContextUtils.clear();
    }

    /**
     * Authenticate if required
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

        // wrap request authentication
        Authentication authRequest = new JwtTokenAuthenticationToken(token);
        // authenticate for result (delegate by ProviderManager)
        Authentication authResult = authenticationManager.authenticate(authRequest);
        // set to security context
        SecurityContextHolder.getContext().setAuthentication(authResult);
    }

    /**
     * Extract Authorization token from request
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(CommonConsts.TOKEN_HEADER_KEY);
        if (StringUtils.isBlank(authHeader)) {
            // 再尝试从query参数中获取
            return this.getTokenFromParameter(request);
        } else {
            return authHeader.replace("Bearer ", "");
        }
    }

    /**
     * 是否跳过认证
     */
    private boolean shouldSkipAuth(String url) {
        return this.isPublicAccessUrl(url) && !this.isAuthOnDemand(url);
    }

    /**
     * isPublicAccessUrl
     */
    private boolean isPublicAccessUrl(String url) {
        boolean r = publicAccessUrls.containsKey(url);
        log.debug("isPublicAccessUrl: {}, url: {}", r, url);
        return r;
    }

    /**
     * isAuthOnDemand
     */
    private boolean isAuthOnDemand(String url) {
        PublicAccess access = publicAccessUrls.get(url);
        boolean r = access != null && access.authOnDemand();
        log.debug("isAuthOnDemand: {}, url: {}", r, url);
        return r;
    }

    /**
     * getTokenFromParameter
     * 先后尝试获取access_token, accessToken, token
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