package com.yeshimin.yeahboot.flowcontrol.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 确保同一次提交意图只执行业务一次。
 *
 * <p>标注的方法必须返回 {@code R<?>}。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等状态记录的保留时长，单位秒。
     */
    long ttlSeconds() default 24 * 60 * 60;
}
