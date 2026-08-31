package com.yeshimin.yeahboot.data.repository;

import cn.hutool.core.collection.CollUtil;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleResEntity;
import com.yeshimin.yeahboot.data.mapper.SysRoleResMapper;
import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class SysRoleResRepo extends BaseRepo<SysRoleResMapper, SysRoleResEntity> {

    /**
     * countByResId
     */
    public long countByResId(Long resId) {
        return lambdaQuery().eq(SysRoleResEntity::getResId, resId).count();
    }

    /**
     * countByMountId
     */
    public long countByMountId(Long mountId) {
        return lambdaQuery().eq(SysRoleResEntity::getMountId, mountId).count();
    }

    /**
     * deleteByRoleId
     */
    public boolean deleteByRoleId(Long roleId) {
        return lambdaUpdate().eq(SysRoleResEntity::getRoleId, roleId).remove();
    }

    /**
     * deleteByMountIds
     */
    public boolean deleteByMountIds(Collection<Long> mountIds) {
        if (CollUtil.isEmpty(mountIds)) {
            return false;
        }
        return lambdaUpdate().in(SysRoleResEntity::getMountId, mountIds).remove();
    }

    /**
     * deleteByResIds
     */
    public boolean deleteByResIds(Collection<Long> resIds) {
        if (CollUtil.isEmpty(resIds)) {
            return false;
        }
        return lambdaUpdate().in(SysRoleResEntity::getResId, resIds).remove();
    }

    /**
     * findListByRoleId
     */
    public List<SysRoleResEntity> findListByRoleId(Long roleId) {
        return lambdaQuery().eq(SysRoleResEntity::getRoleId, roleId).list();
    }

    /**
     * findListByRoleIds
     */
    public List<SysRoleResEntity> findListByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return lambdaQuery().in(SysRoleResEntity::getRoleId, roleIds).list();
    }

    /**
     * createRoleResRelations
     */
    public boolean createRoleResRelations(Long roleId, Collection<Long> resIds) {
        return this.createRoleResRelations(roleId, resIds, 0L);
    }

    /**
     * createRoleResRelations
     */
    public boolean createRoleResRelations(Long roleId, Collection<Long> resIds, Long mountId) {
        if (resIds == null || resIds.isEmpty()) {
            return false;
        }
        List<SysRoleResEntity> list = resIds.stream().map(resId -> {
            SysRoleResEntity entity = new SysRoleResEntity();
            entity.setRoleId(roleId);
            entity.setResId(resId);
            entity.setMountId(mountId);
            return entity;
        }).collect(Collectors.toList());
        return saveBatch(list);
    }
}
