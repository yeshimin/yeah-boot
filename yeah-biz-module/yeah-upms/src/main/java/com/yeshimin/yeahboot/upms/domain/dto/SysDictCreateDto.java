package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 系统字典-创建-DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictCreateDto extends BaseDomain {

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 编码
     */
    @NotBlank(message = "编码不能为空")
    private String code;

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 值
     */
    private String value;

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
