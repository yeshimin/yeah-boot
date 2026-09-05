package com.yeshimin.yeahboot.notification.repository;

import com.yeshimin.yeahboot.common.repository.base.BaseRepo;
import com.yeshimin.yeahboot.notification.domain.entity.NotifSmsApiLogEntity;
import com.yeshimin.yeahboot.notification.mapper.NotifSmsApiLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NotifSmsApiLogRepo extends BaseRepo<NotifSmsApiLogMapper, NotifSmsApiLogEntity> {

    /**
     * createOne
     */
    public NotifSmsApiLogEntity createOne(String request, String response) {
        NotifSmsApiLogEntity entity = new NotifSmsApiLogEntity();
        entity.setRequest(request);
        entity.setResponse(response);
        super.save(entity);
        return entity;
    }
}
