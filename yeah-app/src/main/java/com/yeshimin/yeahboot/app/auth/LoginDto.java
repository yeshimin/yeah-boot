package com.yeshimin.yeahboot.app.auth;

import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
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
    private String mobile;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 终端
     */
    @EnumValue(enumClass = AuthTerminalEnum.class, message = "终端不正确")
    private String terminal;
}
