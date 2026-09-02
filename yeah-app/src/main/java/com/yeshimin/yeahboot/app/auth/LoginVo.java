package com.yeshimin.yeahboot.app.auth;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录-VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginVo extends BaseDomain {

    /**
     * Token
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String token;
}
