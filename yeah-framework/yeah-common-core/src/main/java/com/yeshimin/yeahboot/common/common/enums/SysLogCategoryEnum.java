package com.yeshimin.yeahboot.common.common.enums;

import com.yeshimin.yeahboot.common.common.enums.base.IValueEnum;
import lombok.Getter;

/**
 * 系统日志类别：0-无 1-鉴权相关 2-数据操作 3-定时任务 4-上传下载
 */
@Getter
public enum SysLogCategoryEnum implements IValueEnum {

    /**
     * 无，不区分类别
     */
    NONE("0", "无"),

    /**
     * 鉴权相关
     */
    AUTH("1", "鉴权相关"),

    /**
     * 数据操作
     */
    DATA("2", "数据操作"),

    /**
     * 定时任务
     */
    JOB("3", "定时任务"),

    /**
     * 上传下载
     */
    FILE("4", "上传下载");

    private final String value;
    private final String desc;

    SysLogCategoryEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
