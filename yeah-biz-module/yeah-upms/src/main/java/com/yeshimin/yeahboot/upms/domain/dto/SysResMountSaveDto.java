package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysResMountSaveDto extends BaseDomain {

    /**
     * 视图资源ID
     */
    @NotNull(message = "视图资源ID不能为空")
    private Long viewResId;

    /**
     * （接口资源）挂载项集合
     */
    @Valid
    @NotNull(message = "（接口资源）挂载项集合不能为空")
    private List<SysResMountItemDto> items;
}
