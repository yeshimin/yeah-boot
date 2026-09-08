package com.yeshimin.yeahboot.admin.auth;

import cn.hutool.core.util.BooleanUtil;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.auth.common.config.security.PublicAccess;
import com.yeshimin.yeahboot.auth.domain.vo.CaptchaVo;
import com.yeshimin.yeahboot.auth.service.CaptchaService;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.base.BaseController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.flowcontrol.enums.GroupType;
import com.yeshimin.yeahboot.flowcontrol.ratelimit.RateLimit;
import com.yeshimin.yeahboot.upms.domain.dto.LoginDto;
import com.yeshimin.yeahboot.upms.domain.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final DynamicConfigService dynamicConfigService;

    /**
     * 登录
     */
    @PublicAccess
    @RateLimit(groupType = GroupType.IP, limitGroupCount = 20, timeWindow = 60_000,
            description = "同一IP一分钟最多登录20次")
    @SysLog(value = "登录", category = SysLogCategoryEnum.AUTH)
    @PostMapping("/login")
    public R<LoginVo> login(@Valid @RequestBody LoginDto dto) {
        if (BooleanUtil.isTrue(this.isCaptchaEnabled())) {
            captchaService.checkCaptcha(dto.getKey(), dto.getCode());
        }
        return R.ok(adminAuthService.login(dto));
    }

    /**
     * 解除登录限制
     */
    @PreAuthorize("@pms.hasPermission('api:admin:auth:clearLoginLimit')")
    @SysLog(value = "解除登录限制", category = SysLogCategoryEnum.AUTH)
    @PostMapping("/clearLoginLimit")
    public R<Void> clearLoginLimit(@Valid @RequestBody ClearLoginLimitDto dto) {
        adminAuthService.clearLoginLimit(dto);
        return R.ok();
    }

    /**
     * 图形验证码
     */
    @PublicAccess
    @GetMapping("/captcha")
    public R<CaptchaVo> captcha() {
        Boolean captchaEnabled = this.isCaptchaEnabled();
        CaptchaVo vo = captchaEnabled ? captchaService.generateCaptcha() : new CaptchaVo();
        vo.setEnabled(captchaEnabled);
        return R.ok(vo);
    }

    private Boolean isCaptchaEnabled() {
        return dynamicConfigService.getBoolean(SysConfigEnum.CAPTCHA_ENABLED);
    }
}
