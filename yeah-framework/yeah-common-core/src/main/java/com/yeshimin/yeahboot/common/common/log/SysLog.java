package com.yeshimin.yeahboot.common.common.log;

import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.enums.SysLogTriggerTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SysLog {

    String value() default "";

    // 触发类型：1-系统触发 2-用户触发
    SysLogTriggerTypeEnum triggerType() default SysLogTriggerTypeEnum.SYSTEM;

    // 事件类型：0-无 1-鉴权相关（登录、登出、续期等） 2-数据操作 3-定时任务 4-上传下载
    SysLogCategoryEnum category() default SysLogCategoryEnum.NONE;
}
