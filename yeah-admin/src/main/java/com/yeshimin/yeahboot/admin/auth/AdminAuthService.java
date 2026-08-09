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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 鉴权服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final SysUserRepo sysUserRepo;

    private final PasswordService passwordService;
    private final TerminalAndTokenControlService controlService;

    /**
     * 登录
     */
    public LoginVo login(LoginDto loginDto) {
        AuthenticateDto authenticateDto = new AuthenticateDto();
        authenticateDto.setUsername(loginDto.getUsername());
        authenticateDto.setPassword(loginDto.getPassword());
        AuthenticateVo authenticateVo = this.authenticate(authenticateDto);
        if (!authenticateVo.getSuccess()) {
            throw new BaseException(ErrorCodeEnum.FAIL);
        }

        String userId = String.valueOf(authenticateVo.getUserId());
        String subValue = AuthSubjectEnum.ADMIN.getValue();
        String termValue = StrUtil.isNotBlank(loginDto.getTerminal()) ?
                loginDto.getTerminal() : AuthTerminalEnum.WEB.getValue();

        String token = controlService.doControl(userId, subValue, termValue);

        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        loginVo.setUsername(loginDto.getUsername());
        return loginVo;
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
