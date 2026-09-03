package com.yeshimin.yeahboot.admin.auth;

import com.yeshimin.yeahboot.common.common.enums.AuthTerminalEnum;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 解除登录限制-DTO
 */
@Data
public class ClearLoginLimitDto {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 终端；默认web端
     */
    @EnumValue(enumClass = AuthTerminalEnum.class, message = "终端不正确")
    private String terminal = AuthTerminalEnum.WEB.getValue();
}
