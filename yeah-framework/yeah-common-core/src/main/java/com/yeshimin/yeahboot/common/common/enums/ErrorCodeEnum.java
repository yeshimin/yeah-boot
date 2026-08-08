package com.yeshimin.yeahboot.common.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCodeEnum {

    SUCCESS(0, "成功"),
    FAIL(1, "失败"),
    AUTH_REQUIRED(401, "未认证"),
    FORBIDDEN(403, "权限不足"),
    ;

    private final Integer code;

    private final String desc;

    ErrorCodeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
