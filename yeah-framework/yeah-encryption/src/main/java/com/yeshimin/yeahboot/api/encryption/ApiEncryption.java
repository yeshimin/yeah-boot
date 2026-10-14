package com.yeshimin.yeahboot.api.encryption;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明控制器或接口的报文加密策略。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiEncryption {

    /**
     * 当前控制器或接口的报文加密策略。
     */
    ApiEncryptionPolicy value() default ApiEncryptionPolicy.DEFAULT;
}
