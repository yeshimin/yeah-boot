package com.yeshimin.yeahboot.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yeshimin.yeahboot.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户与组织关联表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_org")
public class SysUserOrgEntity extends BaseEntity<SysUserOrgEntity> {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组织ID
     */
    private Long orgId;
}
