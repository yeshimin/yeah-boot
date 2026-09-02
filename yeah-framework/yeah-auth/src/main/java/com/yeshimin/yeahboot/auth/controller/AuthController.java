package com.yeshimin.yeahboot.auth.controller;

import com.yeshimin.yeahboot.auth.service.AuthService;
import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.base.BaseController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.common.domain.base.ResultVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权相关
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    /**
     * 退出登录
     */
    @SysLog(value = "退出登录", category = SysLogCategoryEnum.AUTH)
    @PostMapping("/logout")
    public R<ResultVo> logout() {
        return R.ok(authService.logout());
    }
}
