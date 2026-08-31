package com.yeshimin.yeahboot.upms.domain.vo;

import com.yeshimin.yeahboot.data.domain.entity.SysResEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统资源节点
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysResTreeNodeVo extends SysResEntity {

    /**
     * 节点key
     */
    private String nodeKey;

    /**
     * 资源ID（等于主键ID字段）
     */
    private Long resId;

    /**
     * 挂载ID
     */
    private Long mountId;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 是否已挂载
     */
    private Boolean mounted;

    /**
     * 子节点集合
     */
    private List<SysResTreeNodeVo> children;
}
