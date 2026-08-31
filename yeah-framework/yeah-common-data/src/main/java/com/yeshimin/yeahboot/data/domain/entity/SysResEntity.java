package com.yeshimin.yeahboot.data.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统资源表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_res")
public class SysResEntity extends ConditionBaseEntity<SysResEntity> {

    /**
     * 类型：1-菜单 2-页面 3-按钮 4-接口 5-分组
     */
    private Integer type;

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 接口资源分组ID，仅接口资源使用
     */
    private Long groupId;

    /**
     * 名称
     */
    private String name;

    /**
     * 权限标识（全局唯一）
     */
    private String permission;

    /**
     * 前端路径
     */
    private String path;

    /**
     * 前端组件
     */
    private String component;

    /**
     * 图标
     */
    private String icon;

    /**
     * 是否外链
     */
    private Boolean isLink;

    /**
     * 外链地址
     */
    private String linkUrl;

    /**
     * 状态：1-启用 2-禁用
     */
    private String status;

    /**
     * 是否可见：1-是 0-否
     */
    private Boolean visible;

    /**
     * 排序：大于等于1
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;
}
