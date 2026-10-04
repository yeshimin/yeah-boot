package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserRoleSetDto extends BaseDomain {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色ID集合
     */
    @NotNull(message = "角色ID集合不能为空")
    private Set<Long> roleIds;
}
