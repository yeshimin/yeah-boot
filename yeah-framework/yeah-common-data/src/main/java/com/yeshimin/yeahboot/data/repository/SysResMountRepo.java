package com.yeshimin.yeahboot.data.repository;

import cn.hutool.core.collection.CollUtil;
import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import com.yeshimin.yeahboot.data.domain.entity.SysResMountEntity;
import com.yeshimin.yeahboot.data.mapper.SysResMountMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class SysResMountRepo extends BaseRepo<SysResMountMapper, SysResMountEntity> {

    /**
     * countByViewResId
     */
    public long countByViewResId(Long viewResId) {
        return lambdaQuery().eq(SysResMountEntity::getViewResId, viewResId).count();
    }

    /**
     * countByApiResId
     */
    public long countByApiResId(Long apiResId) {
        return lambdaQuery().eq(SysResMountEntity::getApiResId, apiResId).count();
    }

    /**
     * findListByViewResId
     */
    public List<SysResMountEntity> findListByViewResId(Long viewResId) {
        return lambdaQuery()
                .eq(SysResMountEntity::getViewResId, viewResId)
                .orderByAsc(SysResMountEntity::getSort)
                .orderByAsc(SysResMountEntity::getId)
                .list();
    }

    /**
     * findListByViewResIds
     */
    public List<SysResMountEntity> findListByViewResIds(Collection<Long> viewResIds) {
        if (CollUtil.isEmpty(viewResIds)) {
            return new ArrayList<>();
        }
        return lambdaQuery()
                .in(SysResMountEntity::getViewResId, viewResIds)
                .orderByAsc(SysResMountEntity::getSort)
                .orderByAsc(SysResMountEntity::getId)
                .list();
    }

    /**
     * findListAll
     */
    public List<SysResMountEntity> findListAll() {
        return lambdaQuery()
                .orderByAsc(SysResMountEntity::getSort)
                .orderByAsc(SysResMountEntity::getId)
                .list();
    }

    /**
     * deleteByViewResId
     */
    public boolean deleteByViewResId(Long viewResId) {
        return lambdaUpdate().eq(SysResMountEntity::getViewResId, viewResId).remove();
    }

    /**
     * deleteByIds
     */
    public boolean deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return false;
        }
        return lambdaUpdate().in(SysResMountEntity::getId, ids).remove();
    }
}
