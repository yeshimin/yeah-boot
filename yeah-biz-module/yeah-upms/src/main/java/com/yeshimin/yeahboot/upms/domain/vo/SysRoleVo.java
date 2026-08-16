package com.yeshimin.yeahboot.upms.domain.vo;

import com.yeshimin.yeahboot.common.domain.base.IdNameStatusVo;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleVo extends SysRoleEntity {

    /**
     * 资源
     */
    private List<IdNameStatusVo> resources;
}
