package com.yeshimin.yeahboot.flowcontrol.idempotent;

import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.common.service.CacheService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    private static final String IDEMPOTENCY_KEY = "00000000-0000-4000-8000-000000000001";

    @Mock
    private CacheService cacheService;

    @Mock
    private Idempotent idempotent;

    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setUp() {
        idempotencyAspect = new IdempotencyAspect(cacheService);
        when(idempotent.ttlSeconds()).thenReturn(300L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        request.addHeader(IdempotencyAspect.HEADER_NAME, IDEMPOTENCY_KEY);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void executesBusinessMethodOnlyAfterAcquiringKey() throws Throwable {
        ProceedingJoinPoint joinPoint = this.joinPointWithArgs(Collections.singletonMap("name", "first"));
        when(cacheService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(R.ok(1L));

        Object result = idempotencyAspect.around(joinPoint, idempotent);

        assertEquals(1L, ((R<?>) result).getData());
        verify(joinPoint).proceed();
        verify(cacheService).set(anyString(), anyString(), eq(300L));
    }

    @Test
    void rejectsSameKeyWithDifferentRequestPayload() throws Throwable {
        ProceedingJoinPoint joinPoint = this.joinPointWithArgs(Collections.singletonMap("name", "second"));
        when(cacheService.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestHash(this.sha256(JSON.toJSONString(new Object[]{Collections.singletonMap("name", "first")})));
        record.setStatus(IdempotencyRecord.Status.PROCESSING);
        when(cacheService.get(anyString())).thenReturn(JSON.toJSONString(record));

        assertThrows(BaseException.class, () -> idempotencyAspect.around(joinPoint, idempotent));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void releasesKeyWhenBusinessMethodFails() throws Throwable {
        ProceedingJoinPoint joinPoint = this.joinPointWithArgs(Collections.singletonMap("name", "first"));
        when(cacheService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("failed"));

        assertThrows(IllegalStateException.class, () -> idempotencyAspect.around(joinPoint, idempotent));
        verify(cacheService).delete(anyString());
    }

    private ProceedingJoinPoint joinPointWithArgs(Object argument) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{argument});
        return joinPoint;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
