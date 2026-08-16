package com.yeshimin.yeahboot.upms.domain.vo;

import com.yeshimin.yeahboot.common.domain.base.IdNameStatusVo;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserVo extends SysUserEntity {

    /**
     * 岗位
     */
    private List<IdNameStatusVo> posts;

    /**
     * 组织
     */
    private List<IdNameStatusVo> orgs;

    /**
     * 角色
     */
    private List<IdNameStatusVo> roles;
}
