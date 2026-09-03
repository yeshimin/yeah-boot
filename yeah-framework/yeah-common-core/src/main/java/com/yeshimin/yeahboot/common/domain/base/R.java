package com.yeshimin.yeahboot.common.domain.base;

import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.log.MdcLogFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.MDC;

@Data
@EqualsAndHashCode(callSuper = true)
public class R<T> extends BaseDomain {

    private Integer code;

    private String message;

    private T data;

    /**
     * 请求链路标识，与日志MDC中的reqId保持一致
     */
    private String traceId;

    private static final Object EMPTY_DATA = new Object();

    // ================================================================================

    public static <T> R<T> ok() {
        return R.ok(ErrorCodeEnum.SUCCESS);
    }

    public static <T> R<T> ok(T data) {
        return R.ok(ErrorCodeEnum.SUCCESS, data);
    }

    public static <T> R<T> ok(String message) {
        return new R<>(ErrorCodeEnum.SUCCESS.getCode(), message, null);
    }

    public static <T> R<T> ok(ErrorCodeEnum errorCodeEnum) {
        return new R<>(errorCodeEnum.getCode(), errorCodeEnum.getDesc(), null);
    }

    public static <T> R<T> ok(ErrorCodeEnum errorCodeEnum, T data) {
        return new R<>(errorCodeEnum.getCode(), errorCodeEnum.getDesc(), data);
    }

    // ================================================================================

    public static <T> R<T> fail() {
        return R.fail(ErrorCodeEnum.FAIL);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(ErrorCodeEnum.FAIL.getCode(), message, null);
    }

    public static <T> R<T> fail(ErrorCodeEnum errorCodeEnum) {
        return new R<>(errorCodeEnum.getCode(), errorCodeEnum.getDesc(), null);
    }

    public static <T> R<T> fail(ErrorCodeEnum errorCodeEnum, String message) {
        return new R<>(errorCodeEnum.getCode(), message, null);
    }

    public static <T> R<T> fail(ErrorCodeEnum errorCodeEnum, T data) {
        return new R<>(errorCodeEnum.getCode(), errorCodeEnum.getDesc(), data);
    }

    // ================================================================================

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get(MdcLogFilter.MDC_REQ_ID);
    }
}
