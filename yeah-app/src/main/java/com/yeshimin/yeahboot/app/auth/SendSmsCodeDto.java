package com.yeshimin.yeahboot.app.auth;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 发送短信验证码-DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SendSmsCodeDto extends BaseDomain {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @SensitiveData(type = SensitiveType.MOBILE, scenes = SensitiveScene.LOG)
    private String mobile;
}
