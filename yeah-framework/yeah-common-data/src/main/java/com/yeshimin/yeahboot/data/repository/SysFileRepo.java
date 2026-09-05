package com.yeshimin.yeahboot.data.repository;

import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import com.yeshimin.yeahboot.data.domain.entity.SysFileEntity;
import com.yeshimin.yeahboot.data.mapper.SysFileMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SysFileRepo extends BaseRepo<SysFileMapper, SysFileEntity> {

    /**
     * findOneByFileKey
     */
    public SysFileEntity findOneByFileKey(String fileKey) {
        return lambdaQuery().eq(SysFileEntity::getFileKey, fileKey).one();
    }
}
