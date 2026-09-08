package com.yeshimin.yeahboot.data.repository;

import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import com.yeshimin.yeahboot.data.domain.entity.SysConfigEntity;
import com.yeshimin.yeahboot.data.mapper.SysConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SysConfigRepo extends BaseRepo<SysConfigMapper, SysConfigEntity> {

    /**
     * 查询参数键是否存在
     */
    public long countByConfigKey(String configKey) {
        return this.lambdaQuery().eq(SysConfigEntity::getConfigKey, configKey).count();
    }

    /**
     * 查询全部启用参数
     */
    public List<SysConfigEntity> findAllEnabled() {
        return this.lambdaQuery()
                .eq(SysConfigEntity::getStatus, DataStatusEnum.ENABLED.getValue())
                .orderByAsc(SysConfigEntity::getSort, SysConfigEntity::getId)
                .list();
    }
}
