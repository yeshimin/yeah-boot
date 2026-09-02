package com.yeshimin.yeahboot.admin.auth;

import cn.hutool.core.util.BooleanUtil;
import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.auth.common.config.security.PublicAccess;
import com.yeshimin.yeahboot.auth.domain.vo.CaptchaVo;
import com.yeshimin.yeahboot.auth.service.CaptchaService;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.common.properties.YeahBootProperties;
import com.yeshimin.yeahboot.common.controller.base.BaseController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.LoginDto;
import com.yeshimin.yeahboot.upms.domain.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 鉴权相关
 */
@Slf4j
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController extends BaseController {

    private final AdminAuthService adminAuthService;
    private final CaptchaService captchaService;

    private final YeahBootProperties yeahBootProperties;

    /**
     * 登录
     */
    @PublicAccess
    @SysLog(value = "登录", category = SysLogCategoryEnum.AUTH)
    @PostMapping("/login")
    public R<LoginVo> login(@Valid @RequestBody LoginDto dto) {
        if (BooleanUtil.isTrue(yeahBootProperties.getCaptchaEnabled())) {
            captchaService.checkCaptcha(dto.getKey(), dto.getCode());
        }
        return R.ok(adminAuthService.login(dto));
    }

    /**
     * 图形验证码
     */
    @PublicAccess
    @GetMapping("/captcha")
    public R<CaptchaVo> captcha() {
        Boolean captchaEnabled = yeahBootProperties.getCaptchaEnabled();
        CaptchaVo vo = captchaEnabled ? captchaService.generateCaptcha() : new CaptchaVo();
        vo.setEnabled(captchaEnabled);
        return R.ok(vo);
    }
}
