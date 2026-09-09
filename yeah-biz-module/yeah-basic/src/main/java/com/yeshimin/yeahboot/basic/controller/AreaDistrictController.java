package com.yeshimin.yeahboot.basic.controller;

import com.yeshimin.yeahboot.data.domain.entity.AreaDistrictEntity;
import com.yeshimin.yeahboot.data.mapper.AreaDistrictMapper;
import com.yeshimin.yeahboot.data.repository.AreaDistrictRepo;
import com.yeshimin.yeahboot.basic.service.AreaDistrictService;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 区县相关
 */
@RestController
@RequestMapping("/area/district")
public class AreaDistrictController extends CrudController<AreaDistrictMapper, AreaDistrictEntity, AreaDistrictRepo> {

    @Autowired
    private AreaDistrictService areaDistrictService;

    public AreaDistrictController(AreaDistrictRepo areaDistrictRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(areaDistrictRepo);
        setModule("api:admin:areaDistrict");
    }

    // ================================================================================
}
