package com.yeshimin.yeahboot.admin.auth;

import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理后台自注册请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminRegisterDto extends BaseDomain {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度必须在2到32个字符之间")
    private String username;

    /**
     * 前端首轮哈希后的密码
     */
    @NotBlank(message = "密码不能为空")
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    @NotBlank(message = "验证码标识不能为空")
    private String key;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
