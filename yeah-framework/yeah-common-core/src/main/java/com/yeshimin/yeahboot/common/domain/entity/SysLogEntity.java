package com.yeshimin.yeahboot.common.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统日志表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLogEntity extends ConditionBaseEntity<SysLogEntity> {

    /**
     * 触发类型：1-系统触发 2-用户触发
     */
    private Integer triggerType;

    /**
     * 事件类型：0-无 1-鉴权相关（登录、登出、续期等） 2-数据操作 3-定时任务 4-上传下载
     */
    private Integer category;

    /**
     * 事件
     */
    private String event;

    /**
     * 输入
     */
    private String input;

    /**
     * 输出
     */
    @TableField("`output`")
    private String output;

    /**
     * 耗时（毫秒）
     */
    private Integer time;

    /**
     * 是否成功：1-成功 0-失败
     */
    private Integer success;

    /**
     * 额外信息（json字符串）
     */
    private String extra;

    /**
     * 备注信息
     */
    private String comment;
}
