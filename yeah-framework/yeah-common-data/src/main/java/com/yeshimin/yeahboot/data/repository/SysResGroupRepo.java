package com.yeshimin.yeahboot.data.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import com.yeshimin.yeahboot.data.domain.entity.SysResGroupEntity;
import com.yeshimin.yeahboot.data.mapper.SysResGroupMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SysResGroupRepo extends BaseRepo<SysResGroupMapper, SysResGroupEntity> {

    /**
     * countByParentId
     */
    public long countByParentId(Long parentId) {
        return lambdaQuery().eq(SysResGroupEntity::getParentId, parentId).count();
    }

    /**
     * countByParentIdAndName
     */
    public long countByParentIdAndName(Long parentId, String name) {
        return this.countByParentIdAndName(parentId, name, null);
    }

    /**
     * countByParentIdAndName
     */
    public long countByParentIdAndName(Long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<SysResGroupEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysResGroupEntity::getParentId, parentId == null ? 0L : parentId);
        wrapper.eq(SysResGroupEntity::getName, name);
        if (excludeId != null) {
            wrapper.ne(SysResGroupEntity::getId, excludeId);
        }
        return count(wrapper);
    }
}
