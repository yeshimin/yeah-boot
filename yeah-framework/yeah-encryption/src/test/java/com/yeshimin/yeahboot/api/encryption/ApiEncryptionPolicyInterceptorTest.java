package com.yeshimin.yeahboot.api.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 接口级报文加密策略拦截器测试。
 */
class ApiEncryptionPolicyInterceptorTest {

    /** 解析当前接口加密策略的模拟组件。 */
    private ApiEncryptionPolicyResolver policyResolver;

    /** 被测的 MVC 策略拦截器。 */
    private ApiEncryptionPolicyInterceptor interceptor;

    /** 用于模拟 Spring MVC 方法处理器的对象。 */
    private HandlerMethod handlerMethod;

    /**
     * 为每个用例构造独立的拦截器与处理器模拟对象。
     */
    @BeforeEach
    void setUp() {
        policyResolver = mock(ApiEncryptionPolicyResolver.class);
        interceptor = new ApiEncryptionPolicyInterceptor(policyResolver);
        handlerMethod = mock(HandlerMethod.class);
    }

    /**
     * OPTIONAL 策略允许迁移期客户端继续发送明文请求。
     */
    @Test
    void optionalPolicyAllowsPlaintextRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        when(policyResolver.resolve(request, handlerMethod)).thenReturn(ApiEncryptionPolicy.OPTIONAL);

        boolean handled = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod);

        assertTrue(handled);
        assertNull(request.getAttribute(ApiEncryptionAttributes.EXPECTED_ATTRIBUTE));
    }

    /**
     * OPTIONAL 策略同时保留加密请求的解密和响应加密上下文。
     */
    @Test
    void optionalPolicyAcceptsEncryptedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        request.addHeader(ApiEncryptionAttributes.MARKER_HEADER, ApiEncryptionAttributes.MARKER_VALUE_V1);
        when(policyResolver.resolve(request, handlerMethod)).thenReturn(ApiEncryptionPolicy.OPTIONAL);

        boolean handled = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod);

        assertTrue(handled);
        assertEquals(Boolean.TRUE, request.getAttribute(ApiEncryptionAttributes.EXPECTED_ATTRIBUTE));
    }
}
