package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 系统日志触发类型：1-系统触发 2-用户触发
 */
@Getter
public enum SysLogTriggerTypeEnum implements IValueEnum {

    /**
     * 系统触发
     */
    SYSTEM("1", "系统触发"),

    /**
     * 用户触发
     */
    USER("2", "用户触发");

    private final String value;
    private final String desc;

    SysLogTriggerTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
