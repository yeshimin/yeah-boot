package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 授权相关-终端枚举
 */
@Getter
public enum AuthTerminalEnum implements IValueEnum {

    // web
    WEB("web", "web端"),
    // app
    APP("app", "app端"),
    // api
    API("api", "api端（比如postman、apifox等api工具调用场景）");

    private final String value;
    private final String desc;

    AuthTerminalEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static AuthTerminalEnum of(String value) {
        for (AuthTerminalEnum e : values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }
}
