package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 重置用户密码-DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserResetPasswordDto extends BaseDomain {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;
}
