package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
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
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    /**
     * 验证码key
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String key;

    /**
     * 验证码
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String code;

    /**
     * 终端
     */
    @EnumValue(enumClass = AuthTerminalEnum.class, message = "终端不正确")
    private String terminal;
}
