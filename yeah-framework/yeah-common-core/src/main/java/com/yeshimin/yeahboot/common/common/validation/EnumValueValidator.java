package com.yeshimin.yeahboot.common.common.validation;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * 枚举值校验器
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private Class<? extends IValueEnum> enumClass;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        IValueEnum[] enumConstants = enumClass.getEnumConstants();
        return Arrays.stream(enumConstants)
                .anyMatch(enumConstant -> enumConstant.getValue().equals(String.valueOf(value)));
    }
}
