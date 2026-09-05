package com.yeshimin.yeahboot.data.repository;

import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import com.yeshimin.yeahboot.data.mapper.AppUserMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepo extends BaseRepo<AppUserMapper, AppUserEntity> {

    /**
     * findOneByMobile
     */
    public AppUserEntity findOneByMobile(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new IllegalArgumentException("phone不能为空");
        }
        return this.lambdaQuery().eq(AppUserEntity::getMobile, phone).one();
    }
}
