package com.yeshimin.yeahboot.admin.controller;

import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import com.yeshimin.yeahboot.data.mapper.AppUserMapper;
import com.yeshimin.yeahboot.data.repository.AppUserRepo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin端-app用户管理
 */
@RestController
@RequestMapping("/admin/appUser")
public class AdminAppUserController extends CrudController<AppUserMapper, AppUserEntity, AppUserRepo> {

    public AdminAppUserController(AppUserRepo repo) {
        super(repo);
        setModule("api:admin:appUser")
                .disableCreate()
                .disableQuery()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================
}
