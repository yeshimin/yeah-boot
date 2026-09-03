package com.yeshimin.yeahboot.data.domain.vo;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户运行时鉴权角色和资源-VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserAuthResourceVo extends BaseDomain {

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 权限标识
     */
    private String permission;
}
