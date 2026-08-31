package com.yeshimin.yeahboot.upms.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class SysResApiTreeNodeVo {

    /**
     * 节点key
     */
    private String nodeKey;

    /**
     * ID
     */
    private Long id;

    /**
     * 资源ID
     */
    private Long resId;

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 名称
     */
    private String name;

    /**
     * 类型：1-菜单 2-页面 3-按钮 4-接口 5-分组
     */
    private Integer type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 状态：1-启用 2-禁用
     */
    private String status;

    /**
     * 排序：大于等于1
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 子节点集合
     */
    private List<SysResApiTreeNodeVo> children;
}
