package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.upms.common.enums.ResTypeEnum;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysResCreateDto extends BaseDomain {

    /**
     * 类型：1-菜单 2-页面 3-按钮 4-接口 5-分组
     */
    @EnumValue(enumClass = ResTypeEnum.class)
    @NotNull(message = "类型不能为空")
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
     * 名称（父下唯一）
     */
    @NotBlank(message = "名称不能为空")
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
    @EnumValue(enumClass = DataStatusEnum.class)
    private String status;

    /**
     * 是否可见：1-是 0-否
     */
    private Boolean visible;

    /**
     * 排序：大于等于1
     */
    @Min(value = 1, message = "排序必须大于等于1")
    private Integer sort;

    /**
     * 备注
     */
    private String remark;
}
