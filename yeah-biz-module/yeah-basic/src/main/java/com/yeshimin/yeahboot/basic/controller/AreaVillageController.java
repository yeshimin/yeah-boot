package com.yeshimin.yeahboot.basic.controller;

import com.yeshimin.yeahboot.data.domain.entity.AreaVillageEntity;
import com.yeshimin.yeahboot.data.mapper.AreaVillageMapper;
import com.yeshimin.yeahboot.data.repository.AreaVillageRepo;
import com.yeshimin.yeahboot.basic.service.AreaVillageService;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 乡村相关
 */
@RestController
@RequestMapping("/area/village")
public class AreaVillageController extends CrudController<AreaVillageMapper, AreaVillageEntity, AreaVillageRepo> {

    @Autowired
    private AreaVillageService areaVillageService;

    public AreaVillageController(AreaVillageRepo areaVillageRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(areaVillageRepo);
        setModule("api:admin:areaVillage")
                .disableCreate()
                .disableQuery()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================
}
