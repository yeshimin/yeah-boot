package com.yeshimin.yeahboot.admin.auth;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.auth.domain.vo.CaptchaVo;
import com.yeshimin.yeahboot.auth.service.CaptchaService;
import com.yeshimin.yeahboot.auth.service.TerminalAndTokenControlService;
import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.common.utils.ClientIpUtils;
import com.yeshimin.yeahboot.common.service.PasswordService;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import com.yeshimin.yeahboot.data.repository.SysRoleRepo;
import com.yeshimin.yeahboot.data.repository.SysUserRepo;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.upms.domain.dto.AuthenticateDto;
import com.yeshimin.yeahboot.upms.domain.dto.LoginDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysUserCreateDto;
import com.yeshimin.yeahboot.upms.domain.vo.AuthenticateVo;
import com.yeshimin.yeahboot.upms.domain.vo.LoginVo;
import com.yeshimin.yeahboot.upms.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 鉴权服务
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final SysUserRepo sysUserRepo;
    private final SysRoleRepo sysRoleRepo;
    private final SysUserService sysUserService;

    private final PasswordService passwordService;
    private final TerminalAndTokenControlService controlService;
    private final AdminLoginAttemptService loginAttemptService;
    private final DynamicConfigService dynamicConfigService;
    private final CaptchaService captchaService;

    /**
     * 登录
     */
    public LoginVo login(LoginDto loginDto) {
        String termValue = StrUtil.blankToDefault(loginDto.getTerminal(), AuthTerminalEnum.WEB.getValue());

        // 检查IP白名单
        String clientIp = ClientIpUtils.getCurrentClientIp();
        loginAttemptService.checkLoginAllowed(clientIp);

        // 密码校验前先检查当前用户名、终端和可选IP维度是否已被临时锁定
        loginAttemptService.checkLocked(loginDto.getUsername(), termValue, clientIp);

        AuthenticateDto authenticateDto = new AuthenticateDto();
        authenticateDto.setUsername(loginDto.getUsername());
        authenticateDto.setPassword(loginDto.getPassword());
        AuthenticateVo authenticateVo = this.authenticate(authenticateDto);
        if (!authenticateVo.getSuccess()) {
            long remaining = loginAttemptService.recordFailure(loginDto.getUsername(), termValue, clientIp);
            if (remaining <= 0) {
                throw new BaseException(ErrorCodeEnum.FAIL,
                        "登录失败次数过多，请" + loginAttemptService.getLockSeconds() + "秒后重试");
            }
            throw new BaseException(ErrorCodeEnum.FAIL, "用户名或密码错误，剩余可尝试次数：" + remaining);
        }

        // 登录认证成功，清除当前维度的历史失败记录
        loginAttemptService.clearCurrent(loginDto.getUsername(), termValue, clientIp);

        String userId = String.valueOf(authenticateVo.getUserId());
        String subValue = AuthSubjectEnum.ADMIN.getValue();

        String token = controlService.doControl(userId, subValue, termValue);

        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        loginVo.setUsername(loginDto.getUsername());
        return loginVo;
    }

    /**
     * 解除指定用户名和终端的登录失败次数及临时锁定
     */
    public void clearLoginLimit(ClearLoginLimitDto dto) {
        loginAttemptService.clearAll(dto.getUsername(), dto.getTerminal());
    }

    /**
     * 生成管理后台自注册验证码
     */
    public CaptchaVo generateRegisterCaptcha() {
        this.checkRegisterEnabled();
        CaptchaVo vo = captchaService.generateCaptcha();
        vo.setEnabled(true);
        return vo;
    }

    /**
     * 管理后台自注册
     */
    public void register(AdminRegisterDto dto) {
        this.checkRegisterEnabled();
        captchaService.checkCaptcha(dto.getKey(), dto.getCode());

        String roleCode = dynamicConfigService.getString(SysConfigEnum.ADMIN_AUTH_LOGIN_REGISTER_DEFAULT_ROLE_CODE);
        if (StrUtil.isBlank(roleCode)) {
            throw new BaseException("自注册默认角色未配置");
        }
        SysRoleEntity role = sysRoleRepo.findOneByCode(roleCode);
        if (role == null || !DataStatusEnum.ENABLED.equalsValue(role.getStatus())) {
            throw new BaseException("自注册默认角色不可用");
        }

        SysUserCreateDto createDto = new SysUserCreateDto();
        createDto.setUsername(dto.getUsername());
        createDto.setPassword(dto.getPassword());
        createDto.setStatus(DataStatusEnum.ENABLED.getValue());
        createDto.setRoleIds(Collections.singleton(role.getId()));
        sysUserService.create(createDto);
    }

    // ================================================================================

    /**
     * 在注册入口执行前统一校验动态注册开关。
     */
    private void checkRegisterEnabled() {
        if (!this.isRegisterEnabled()) {
            throw new BaseException("当前未开放注册");
        }
    }

    /**
     * 读取管理后台匿名注册是否启用。
     */
    private boolean isRegisterEnabled() {
        return BooleanUtil.isTrue(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_AUTH_LOGIN_REGISTER_ENABLED));
    }

    // ================================================================================

    /**
     * 认证（账号密码方式）
     */
    private AuthenticateVo authenticate(AuthenticateDto authenticateDto) {
        AuthenticateVo vo = new AuthenticateVo();

        // 查找系统用户
        SysUserEntity sysUser = sysUserRepo.findOneByUsername(authenticateDto.getUsername());
        if (sysUser == null) {
            vo.setSuccess(false);
        } else {
            vo.setUserId(sysUser.getId());
            vo.setUsername(sysUser.getUsername());

            // 校验密码
            boolean success = passwordService.validatePassword(authenticateDto.getPassword(), sysUser.getPassword());
            if (success && !DataStatusEnum.ENABLED.equalsValue(sysUser.getStatus())) {
                throw new BaseException("用户已禁用");
            }
            vo.setSuccess(success);
        }
        return vo;
    }
}
