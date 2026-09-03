package com.yeshimin.yeahboot.upms.service;

import com.yeshimin.yeahboot.common.domain.entity.SysLogEntity;
import com.yeshimin.yeahboot.data.repository.SysLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysLogRepo sysLogRepo;

    /**
     * 日志详情
     */
    public SysLogEntity detail(Long id) {
        return sysLogRepo.getOneById(id);
    }
}
