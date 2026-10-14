package com.yeshimin.yeahboot.api.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 在 DTO 绑定前将加密 JSON Envelope 替换为明文 JSON。
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ApiEncryptionRequestBodyAdvice extends RequestBodyAdviceAdapter {

    /** 报文加密协议的部署配置。 */
    private final ApiEncryptionProperties properties;

    /** 负责请求解密、防重放和响应上下文建立的服务。 */
    private final ApiEncryptionService apiEncryptionService;

    /** 用于读取加密 Envelope 的 JSON 工具。 */
    private final ObjectMapper objectMapper;

    /**
     * 仅对已由策略拦截器确认的 JSON 请求体执行解密。
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        HttpServletRequest request = this.currentRequest();
        return properties.isEnabled()
                && request != null
                && Boolean.TRUE.equals(request.getAttribute(ApiEncryptionAttributes.EXPECTED_ATTRIBUTE))
                && MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * 将加密 Envelope 替换为明文 JSON，使后续 DTO 绑定和校验无需感知密码协议。
     */
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        HttpServletRequest request = this.currentRequest();
        if (request == null) {
            throw new ApiEncryptionException("加密报文无效");
        }
        // 限制密文大小后再进入 JSON 解析和密码学处理。
        byte[] raw = StreamUtils.copyToByteArray(inputMessage.getBody());
        if (raw.length > properties.getMaxPayloadBytes()) {
            throw new ApiEncryptionException("加密报文过大");
        }

        ApiEncryptionEnvelope envelope;
        try {
            envelope = objectMapper.readValue(raw, ApiEncryptionEnvelope.class);
        } catch (Exception e) {
            throw new ApiEncryptionException("加密报文无效");
        }
        // 成功解密后替换输入流，后续 DTO 绑定与 @Valid 仍按明文 JSON 工作。
        byte[] plaintext = apiEncryptionService.decryptRequest(envelope, request);
        request.setAttribute(ApiEncryptionAttributes.ENCRYPTED_ATTRIBUTE, Boolean.TRUE);

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(inputMessage.getHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(plaintext.length);
        return new DecryptedHttpInputMessage(headers, plaintext);
    }

    /**
     * 获取当前 Servlet 请求；在非 Web 调用链中返回空。
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    private static final class DecryptedHttpInputMessage implements HttpInputMessage {

        /** 解密后请求体对应的响应头副本。 */
        private final HttpHeaders headers;

        /** 已认证并解密的明文 JSON 字节。 */
        private final byte[] body;

        /**
         * 创建供 Spring JSON Converter 读取的解密请求消息。
         *
         * @param headers 解密后使用的请求头
         * @param body    解密后的 JSON 字节
         */
        private DecryptedHttpInputMessage(HttpHeaders headers, byte[] body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public java.io.InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
