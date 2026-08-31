package com.yeshimin.yeahboot.upms.domain.vo;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserRolesAndResourcesVo extends BaseDomain {

    /**
     * 用户信息
     */
    private SysUserEntity user;

    /**
     * 角色列表
     */
    private Set<String> roles;

    /**
     * 资源列表
     */
    private Set<String> resources;
}
