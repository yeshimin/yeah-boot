package com.yeshimin.yeahboot.upms.domain.vo;

import com.yeshimin.yeahboot.data.domain.entity.SysResGroupEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysResGroupTreeNodeVo extends SysResGroupEntity {

    /**
     * 子节点集合
     */
    private List<SysResGroupTreeNodeVo> children;
}
