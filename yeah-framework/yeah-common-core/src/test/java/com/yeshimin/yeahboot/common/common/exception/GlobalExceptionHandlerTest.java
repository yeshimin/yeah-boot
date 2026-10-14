package com.yeshimin.yeahboot.common.common.exception;

import com.yeshimin.yeahboot.common.common.log.MdcLogFilter;
import com.yeshimin.yeahboot.common.domain.base.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void hidesUnexpectedExceptionMessageAndReturnsTraceId() {
        MDC.put(MdcLogFilter.MDC_REQ_ID, "trace-test-001");

        R<Void> response = exceptionHandler.handleException(
                new IllegalStateException("SQL syntax error near sensitive_table"));

        assertEquals("系统异常，请联系管理员", response.getMessage());
        assertEquals("trace-test-001", response.getTraceId());
        assertFalse(response.getMessage().contains("sensitive_table"));
    }

    @Test
    void hidesRawRequestParsingMessage() {
        R<Void> response = exceptionHandler.handleParamException(
                new HttpMessageNotReadableException("Unexpected token internal_field"));

        assertEquals("请求格式错误", response.getMessage());
        assertFalse(response.getMessage().contains("internal_field"));
    }
}
