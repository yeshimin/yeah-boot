package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 性别枚举
 */
@Getter
public enum GenderEnum implements IValueEnum {

    UNKNOWN("0", "未知"),
    MALE("1", "男"),
    FEMALE("2", "女");

    private final String value;
    private final String desc;

    GenderEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static GenderEnum of(String value) {
        for (GenderEnum e : GenderEnum.values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }
}
