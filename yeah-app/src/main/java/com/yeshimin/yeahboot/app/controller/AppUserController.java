package com.yeshimin.yeahboot.app.controller;

import com.yeshimin.yeahboot.app.service.AppUserService;
import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.common.controller.base.BaseController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * app端用户相关
 */
@Tag(name = "app端用户相关")
@RestController
@RequestMapping("/app/appUser")
@RequiredArgsConstructor
public class AppUserController extends BaseController {

    private final AppUserService appUserService;

    /**
     * 详情
     */
    @Operation(summary = "详情")
    @GetMapping("/detail")
    public R<AppUserEntity> detail() {
        Long userId = WebContextUtils.getUserId();
        return R.ok(appUserService.detail(userId));
    }
}
