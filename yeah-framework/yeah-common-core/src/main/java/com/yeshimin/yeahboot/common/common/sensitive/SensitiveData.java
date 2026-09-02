package com.yeshimin.yeahboot.common.common.sensitive;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据脱敏注解
 * <p>
 * 默认在接口响应和日志输出时进行完全隐藏，可通过type指定脱敏类型，
 * 通过scenes指定生效场景。
 * </p>
 * <p>
 * 示例：{@code @SensitiveData(type = SensitiveType.MOBILE)}
 * </p>
 * <p>
 * 仅日志脱敏：{@code @SensitiveData(scenes = SensitiveScene.LOG)}
 * </p>
 */
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveDataJsonSerializer.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    /**
     * 脱敏类型，默认完全隐藏
     *
     * @return 脱敏类型
     */
    SensitiveType type() default SensitiveType.FULL;

    /**
     * 生效场景：RESP-接口响应，LOG-日志输出；默认两个场景都生效
     *
     * @return 生效场景
     */
    SensitiveScene[] scenes() default {SensitiveScene.RESP, SensitiveScene.LOG};
}
