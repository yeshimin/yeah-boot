package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.common.enums.SysConfigValueTypeEnum;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigUpdateDto extends BaseDomain {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "参数分组不能为空")
    @Size(max = 32, message = "参数分组不能超过32个字符")
    private String groupCode;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 64, message = "参数名称不能超过64个字符")
    private String configName;

    @NotNull(message = "参数值不能为空")
    @Size(max = 1024, message = "参数值不能超过1024个字符")
    private String configValue;

    @NotNull(message = "参数值类型不能为空")
    @EnumValue(enumClass = SysConfigValueTypeEnum.class, message = "参数值类型不正确")
    private Integer valueType;

    @NotNull(message = "状态不能为空")
    @EnumValue(enumClass = DataStatusEnum.class, message = "状态不正确")
    private String status;

    @NotNull(message = "排序不能为空")
    @Min(value = 1, message = "排序必须大于等于1")
    private Integer sort;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
