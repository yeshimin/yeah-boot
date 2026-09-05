package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 数据状态枚举：1-启用 2-禁用
 */
@Getter
public enum DataStatusEnum implements IValueEnum {

    /**
     * 启用
     */
    ENABLED("1", "启用"),
    /**
     * 禁用
     */
    DISABLED("2", "禁用");

    private final String value;
    private final String desc;

    DataStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static DataStatusEnum of(String value) {
        for (DataStatusEnum e : values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }
}
