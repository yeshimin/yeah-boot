package com.yeshimin.yeahboot.api.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 在 MVC 解析 @RequestParam 前解密查询参数。
 */
@Component
@RequiredArgsConstructor
public class ApiEncryptionQueryFilter extends OncePerRequestFilter {

    /** 报文加密协议的部署配置。 */
    private final ApiEncryptionProperties properties;

    /** 负责 Envelope 解密、防重放和响应上下文建立的服务。 */
    private final ApiEncryptionService apiEncryptionService;

    /** 解析查询参数中 Base64URL Envelope 的 JSON 工具。 */
    private final ObjectMapper objectMapper;

    /**
     * 在 MVC 参数绑定前识别并解密 {@code __enc} 查询参数。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅显式携带 __enc 参数的请求进入查询参数解密流程。
        String[] encryptedQueryValues = request.getParameterValues(ApiEncryptionAttributes.QUERY_PARAMETER);
        if (encryptedQueryValues == null) {
            filterChain.doFilter(request, response);
            return;
        }
        // 拒绝混合明文参数与多个 Envelope，避免参数语义被覆盖或产生歧义。
        if (!ApiEncryptionAttributes.MARKER_VALUE_V1.equals(
                request.getHeader(ApiEncryptionAttributes.MARKER_HEADER))
                || request.getParameterMap().size() != 1
                || encryptedQueryValues.length != 1
                || !StringUtils.hasText(encryptedQueryValues[0])) {
            throw new ApiEncryptionException("加密查询参数格式无效");
        }

        // 解密后用 RequestWrapper 替换参数映射，使既有 @RequestParam 绑定无需改造。
        ApiEncryptionEnvelope envelope = this.readEnvelope(encryptedQueryValues[0]);
        byte[] plaintext = apiEncryptionService.decryptRequest(envelope, request);
        request.setAttribute(ApiEncryptionAttributes.ENCRYPTED_ATTRIBUTE, Boolean.TRUE);
        filterChain.doFilter(new DecryptedQueryRequestWrapper(request, plaintext, objectMapper), response);
    }

    /**
     * 解码 Base64URL 查询 Envelope 并限制其大小，避免超长 URL 进入 JSON 解析器。
     */
    private ApiEncryptionEnvelope readEnvelope(String encryptedQuery) {
        try {
            byte[] raw = Base64.getUrlDecoder().decode(encryptedQuery);
            if (raw.length > properties.getMaxPayloadBytes()) {
                throw new ApiEncryptionException("加密查询参数格式无效");
            }
            return objectMapper.readValue(new String(raw, StandardCharsets.UTF_8), ApiEncryptionEnvelope.class);
        } catch (ApiEncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiEncryptionException("加密查询参数格式无效");
        }
    }
}
