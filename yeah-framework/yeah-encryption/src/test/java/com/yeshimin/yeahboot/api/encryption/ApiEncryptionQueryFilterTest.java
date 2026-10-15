package com.yeshimin.yeahboot.api.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 加密查询参数过滤器测试。
 */
class ApiEncryptionQueryFilterTest {

    /**
     * 过滤器阶段的解密失败应返回业务错误，而不是交由安全链误判为认证失败。
     */
    @Test
    void returnsProtocolErrorWhenQueryEnvelopeCannotBeDecrypted() throws Exception {
        ApiEncryptionProperties properties = new ApiEncryptionProperties();
        properties.setMode(ApiEncryptionMode.REQUIRED);
        ApiEncryptionService encryptionService = mock(ApiEncryptionService.class);
        ApiEncryptionQueryFilter filter = new ApiEncryptionQueryFilter(
                properties, encryptionService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/auth/captcha");
        request.addHeader(ApiEncryptionAttributes.MARKER_HEADER, ApiEncryptionAttributes.MARKER_VALUE_V1);
        request.addParameter(ApiEncryptionAttributes.QUERY_PARAMETER,
                Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes(StandardCharsets.UTF_8)));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        when(encryptionService.decryptRequest(any(ApiEncryptionEnvelope.class), same(request)))
                .thenThrow(new ApiEncryptionException("加密报文无效"));

        filter.doFilter(request, response, filterChain);

        assertNull(filterChain.getRequest());
        assertTrue(response.getContentAsString().contains("\"code\":1"));
        assertTrue(response.getContentAsString().contains("加密报文无效"));
    }
}
