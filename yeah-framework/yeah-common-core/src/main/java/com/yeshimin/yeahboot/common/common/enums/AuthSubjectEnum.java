package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 授权相关-主题（子系统/模块）枚举
 */
@Getter
public enum AuthSubjectEnum implements IValueEnum {

    // admin
    ADMIN("admin", "管理端"),
    // app
    APP("app", "App端");

    private final String value;
    private final String desc;

    AuthSubjectEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static AuthSubjectEnum of(String value) {
        for (AuthSubjectEnum e : values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }
}
