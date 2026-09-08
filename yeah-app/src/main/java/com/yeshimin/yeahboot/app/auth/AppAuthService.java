package com.yeshimin.yeahboot.app.auth;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.app.domain.mq.payload.SmsMqPayload;
import com.yeshimin.yeahboot.auth.service.TerminalAndTokenControlService;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.CacheService;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.common.service.IdService;
import com.yeshimin.yeahboot.common.service.PasswordService;
import com.yeshimin.yeahboot.data.common.consts.BizConsts;
import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import com.yeshimin.yeahboot.data.repository.AppUserRepo;
import com.yeshimin.yeahboot.mq.MqMessage;
import com.yeshimin.yeahboot.mq.MqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 鉴权服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthService {

    private final AppUserRepo appUserRepo;

    private final PasswordService passwordService;
    private final TerminalAndTokenControlService controlService;

    private final DynamicConfigService dynamicConfigService;
    private final CacheService cacheService;
    private final IdService idService;

    private final MqPublisher mqPublisher;

    /**
     * 登录
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVo login(LoginDto loginDto) {
        // 查找用户
        AppUserEntity appUser = appUserRepo.findOneByMobile(loginDto.getMobile());
        if (appUser == null) {
            // 用户未找到的情况下，如果是短信验证码登录，则直接创建用户
            if (StrUtil.isNotBlank(loginDto.getSmsCode())) {
                if (this.consumeSmsCode(loginDto.getMobile(), loginDto.getSmsCode())) {
                    // 创建用户
                    appUser = this.createMobileUser(loginDto.getMobile());
                } else {
                    throw new BaseException(ErrorCodeEnum.FAIL, "短信验证码不匹配");
                }
            } else {
                throw new BaseException(ErrorCodeEnum.FAIL, "用户未找到");
            }
        }
        // 用户存在的情况下，直接进行验证
        else {
            // 判断认证方式：短信验证码 或 密码
            if (StrUtil.isNotBlank(loginDto.getSmsCode())) {
                if (!this.consumeSmsCode(loginDto.getMobile(), loginDto.getSmsCode())) {
                    throw new BaseException(ErrorCodeEnum.FAIL, "短信验证码不匹配");
                }
            } else if (StrUtil.isNotBlank(loginDto.getPassword())) {
                if (!passwordService.validatePassword(loginDto.getPassword(), appUser.getPassword())) {
                    throw new BaseException(ErrorCodeEnum.FAIL, "密码不正确");
                }
            } else {
                throw new BaseException(ErrorCodeEnum.FAIL, "至少选择一种认证方式");
            }
        }

        String userId = String.valueOf(appUser.getId());
        String subValue = AuthSubjectEnum.APP.getValue();
        String termValue = StrUtil.isNotBlank(loginDto.getTerminal()) ?
                loginDto.getTerminal() : AuthTerminalEnum.APP.getValue();

        String token = controlService.doControl(userId, subValue, termValue);

        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        return loginVo;
    }

    /**
     * 发送短信验证码
     */
    public void sendSmsCode(SendSmsCodeDto dto) {
        // 生成短信验证码
        Integer smsCodeLength = dynamicConfigService.getInteger(SysConfigEnum.SMS_CODE_LENGTH);
        Integer smsCodeExpSeconds = dynamicConfigService.getInteger(SysConfigEnum.SMS_CODE_EXP_SECONDS);
        String smsCode = RandomUtil.randomNumbers(smsCodeLength);
        // 生成缓存key
        String key = String.format(CommonConsts.APP_SMS_CODE_KEY, dto.getMobile());
        log.debug("smsCode: {}, key: {}", smsCode, key);
        // 执行缓存
        cacheService.set(key, smsCode, smsCodeExpSeconds);
        // 发送短信（异步）
        this.asyncSendSms(smsCode, dto.getMobile());
    }

    // ================================================================================

    /**
     * 异步发送短信验证码
     */
    private void asyncSendSms(String smsCode, String mobile) {
        MqMessage message = new MqMessage();
        message.setTopic(BizConsts.LOGIN_SMS_CODE_TOPIC);
        message.setPayload(SmsMqPayload.of(smsCode, mobile).toJsonString());
        mqPublisher.publish(message);
    }

    /**
     * 消费短信验证码
     * 如果成功，则删除缓存
     */
    private boolean consumeSmsCode(String mobile, String smsCode) {
        String key = String.format(CommonConsts.APP_SMS_CODE_KEY, mobile);
        String code = cacheService.get(key);
        if (StrUtil.isNotBlank(code) && Objects.equals(code, smsCode)) {
            cacheService.delete(key);
            return true;
        }
        return false;
    }

    /**
     * 创建手机用户
     */
    private AppUserEntity createMobileUser(String mobile) {
        AppUserEntity user = new AppUserEntity();
        user.setMobile(mobile);
        user.setAccount(idService.nextEncodedId());
        boolean r = user.insert();
        log.debug("createMobileUser.result: {}, user: {}", r, user);
        return user;
    }
}
