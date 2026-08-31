package com.yeshimin.yeahboot.admin.controller;

import com.yeshimin.yeahboot.admin.service.AdminAppUserService;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import com.yeshimin.yeahboot.data.mapper.AppUserMapper;
import com.yeshimin.yeahboot.data.repository.AppUserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin端-app用户管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/appUser")
public class AdminAppUserController extends CrudController<AppUserMapper, AppUserEntity, AppUserRepo> {

    @Autowired
    private AdminAppUserService service;

    public AdminAppUserController(AppUserRepo repo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(repo);
        super.setModule("admin:appUser")
                .disableCreate()
                .disableQuery()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================
}
