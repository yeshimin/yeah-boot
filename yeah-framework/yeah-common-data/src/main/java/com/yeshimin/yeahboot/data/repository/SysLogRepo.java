package com.yeshimin.yeahboot.data.repository;

import com.yeshimin.yeahboot.common.domain.entity.SysLogEntity;
import com.yeshimin.yeahboot.common.mapper.SysLogMapper;
import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import org.springframework.stereotype.Repository;

@Repository
public class SysLogRepo extends BaseRepo<SysLogMapper, SysLogEntity> {
}
