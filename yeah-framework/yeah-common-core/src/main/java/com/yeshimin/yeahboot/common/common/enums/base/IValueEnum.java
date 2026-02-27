package com.yeshimin.yeahboot.common.common.enums.base;

public interface IValueEnum {

    String getValue();

    default Integer getIntValue() {
        return Integer.parseInt(getValue());
    }

    default boolean equalsValue(Object value) {
        return value != null && getValue().equals(value.toString());
    }
}
