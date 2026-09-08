package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 系统参数值类型
 */
@Getter
public enum SysConfigValueTypeEnum implements IValueEnum {

    /**
     * 字符串
     */
    STRING("1", "字符串"),

    /**
     * 整数
     */
    INTEGER("2", "整数"),

    /**
     * 长整数
     */
    LONG("3", "长整数"),

    /**
     * 布尔值
     */
    BOOLEAN("4", "布尔值");

    private final String value;
    private final String desc;

    SysConfigValueTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static SysConfigValueTypeEnum of(Object value) {
        if (value == null) {
            return null;
        }
        for (SysConfigValueTypeEnum item : values()) {
            if (item.equalsValue(value)) {
                return item;
            }
        }
        return null;
    }
}
