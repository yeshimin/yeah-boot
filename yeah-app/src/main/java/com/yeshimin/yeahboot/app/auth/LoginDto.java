package com.yeshimin.yeahboot.app.auth;

import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 登录-DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginDto extends BaseDomain {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @SensitiveData(type = SensitiveType.MOBILE, scenes = SensitiveScene.LOG)
    private String mobile;

    /**
     * 短信验证码
     * 与【密码】二选一
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String smsCode;

    /**
     * 密码
     * 与【短信验证码】二选一
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    /**
     * 终端
     */
    @EnumValue(enumClass = AuthTerminalEnum.class, message = "终端不正确")
    private String terminal;
}
