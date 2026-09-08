package com.yeshimin.yeahboot.data.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfigEntity extends ConditionBaseEntity<SysConfigEntity> {

    /**
     * 参数分组编码
     */
    private String groupCode;

    /**
     * 参数键
     */
    private String configKey;

    /**
     * 参数名称
     */
    private String configName;

    /**
     * 参数值
     */
    private String configValue;

    /**
     * 值类型：1-字符串 2-整数 3-长整数 4-布尔值
     */
    private Integer valueType;

    /**
     * 状态：1-启用 2-禁用
     */
    private String status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;
}
