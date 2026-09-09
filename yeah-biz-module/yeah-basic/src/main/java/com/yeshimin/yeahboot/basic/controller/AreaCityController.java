package com.yeshimin.yeahboot.basic.controller;

import com.yeshimin.yeahboot.basic.service.AreaCityService;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.data.domain.entity.AreaCityEntity;
import com.yeshimin.yeahboot.data.mapper.AreaCityMapper;
import com.yeshimin.yeahboot.data.repository.AreaCityRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 城市相关
 */
@RestController
@RequestMapping("/area/city")
public class AreaCityController extends CrudController<AreaCityMapper, AreaCityEntity, AreaCityRepo> {

    @Autowired
    private AreaCityService areaCityService;

    public AreaCityController(AreaCityRepo areaCityRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(areaCityRepo);
        super.setModule("api:admin:areaCity");
    }

    // ================================================================================
}
