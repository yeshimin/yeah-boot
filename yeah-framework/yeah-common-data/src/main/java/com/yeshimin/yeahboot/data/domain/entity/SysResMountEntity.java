package com.yeshimin.yeahboot.data.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_res_mount")
public class SysResMountEntity extends ConditionBaseEntity<SysResMountEntity> {

    /**
     * 视图资源ID
     */
    private Long viewResId;

    /**
     * 接口资源ID
     */
    private Long apiResId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;
}
