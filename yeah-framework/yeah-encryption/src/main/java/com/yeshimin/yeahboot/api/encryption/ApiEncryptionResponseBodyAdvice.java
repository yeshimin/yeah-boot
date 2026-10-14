package com.yeshimin.yeahboot.api.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 为已验证的加密请求包装控制器和异常处理器的 JSON 响应。
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ApiEncryptionResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /** 负责响应加密和请求级上下文清理的服务。 */
    private final ApiEncryptionService apiEncryptionService;

    /** 将控制器返回对象序列化为待加密 JSON 的工具。 */
    private final ObjectMapper objectMapper;

    /**
     * 在运行时根据请求上下文决定是否加密，所有常规 JSON 响应均可进入此 Advice。
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 仅为成功建立加密上下文的请求包装响应，并在写出前清理派生响应密钥。
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 响应加密上下文存放在 Servlet request attribute，非 Servlet 场景无法读取。
        if (!(request instanceof ServletServerHttpRequest)) {
            return body;
        }
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        // 仅成功解密请求才会建立响应方向密钥。
        ApiEncryptionContext context = apiEncryptionService.getContext(httpRequest);
        if (context == null) {
            return body;
        }
        if (body instanceof ApiEncryptionResponseEnvelope) {
            apiEncryptionService.clearContext(httpRequest);
            return body;
        }

        try {
            // 先序列化原始业务响应，再将其包装为加密响应 Envelope。
            ApiEncryptionResponseEnvelope envelope = apiEncryptionService.encryptResponse(
                    objectMapper.writeValueAsBytes(body), context);
            response.getHeaders().set(ApiEncryptionAttributes.MARKER_HEADER,
                    ApiEncryptionAttributes.MARKER_VALUE_V1);
            return envelope;
        } catch (Exception e) {
            throw new IllegalStateException("响应加密失败", e);
        } finally {
            apiEncryptionService.clearContext(httpRequest);
        }
    }
}
