package com.yeshimin.yeahboot.api.encryption;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 在控制器参数绑定前执行已解析的接口加密策略。
 */
@Component
@RequiredArgsConstructor
public class ApiEncryptionPolicyInterceptor implements HandlerInterceptor {

    /** 根据注解、YAML 豁免规则和全局模式解析接口加密策略的组件。 */
    private final ApiEncryptionPolicyResolver policyResolver;

    /**
     * 在控制器参数绑定前校验当前接口是否必须使用或禁止使用加密 Envelope。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 此阶段已得到 HandlerMethod，可以读取接口或控制器上的 @ApiEncryption 注解。
        ApiEncryptionPolicy policy = policyResolver.resolve(request, handlerMethod);
        boolean encrypted = Boolean.TRUE.equals(request.getAttribute(ApiEncryptionAttributes.ENCRYPTED_ATTRIBUTE))
                || ApiEncryptionAttributes.MARKER_VALUE_V1.equals(
                        request.getHeader(ApiEncryptionAttributes.MARKER_HEADER));

        // 显式豁免接口拒绝加密报文，避免客户端与服务端协议预期不一致。
        if (policy == ApiEncryptionPolicy.BYPASS) {
            if (encrypted) {
                throw new ApiEncryptionException("当前接口不接受加密报文");
            }
            return true;
        }
        if (policy == ApiEncryptionPolicy.REQUIRED && !encrypted) {
            throw new ApiEncryptionException("当前接口要求加密报文");
        }
        if (encrypted) {
            // 供 RequestBodyAdvice 在 DTO 绑定前识别并解密 JSON Envelope。
            request.setAttribute(ApiEncryptionAttributes.EXPECTED_ATTRIBUTE, Boolean.TRUE);
        }
        return true;
    }
}
