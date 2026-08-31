package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleResSetDto extends BaseDomain {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 视图资源ID集合
     */
    @NotNull(message = "视图资源ID集合不能为空")
    private Set<Long> viewResIds;

    /**
     * 挂载ID集合
     */
    private Set<Long> mountIds;
}
