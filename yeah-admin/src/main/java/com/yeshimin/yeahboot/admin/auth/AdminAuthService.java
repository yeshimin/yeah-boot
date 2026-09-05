package com.yeshimin.yeahboot.admin.auth;

import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.auth.service.TerminalAndTokenControlService;
import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.PasswordService;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import com.yeshimin.yeahboot.data.repository.SysUserRepo;
import com.yeshimin.yeahboot.upms.domain.dto.AuthenticateDto;
import com.yeshimin.yeahboot.upms.domain.dto.LoginDto;
import com.yeshimin.yeahboot.upms.domain.vo.AuthenticateVo;
import com.yeshimin.yeahboot.upms.domain.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 鉴权服务
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final SysUserRepo sysUserRepo;

    private final PasswordService passwordService;
    private final TerminalAndTokenControlService controlService;
    private final AdminLoginAttemptService loginAttemptService;

    /**
     * 登录
     */
    public LoginVo login(LoginDto loginDto) {
        String termValue = StrUtil.blankToDefault(loginDto.getTerminal(), AuthTerminalEnum.WEB.getValue());

        // 密码校验前先检查当前用户名和终端组合是否已被临时锁定
        loginAttemptService.checkLocked(loginDto.getUsername(), termValue);

        AuthenticateDto authenticateDto = new AuthenticateDto();
        authenticateDto.setUsername(loginDto.getUsername());
        authenticateDto.setPassword(loginDto.getPassword());
        AuthenticateVo authenticateVo = this.authenticate(authenticateDto);
        if (!authenticateVo.getSuccess()) {
            long remaining = loginAttemptService.recordFailure(loginDto.getUsername(), termValue);
            if (remaining <= 0) {
                throw new BaseException(ErrorCodeEnum.FAIL,
                        "登录失败次数过多，请" + loginAttemptService.getLockSeconds() + "秒后重试");
            }
            throw new BaseException(ErrorCodeEnum.FAIL, "用户名或密码错误，剩余可尝试次数：" + remaining);
        }

        // 登录认证成功，清除当前维度的历史失败记录
        loginAttemptService.clear(loginDto.getUsername(), termValue);

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
        loginAttemptService.clear(dto.getUsername(), dto.getTerminal());
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
