package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.enums.GenderEnum;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserUpdateMineDto extends BaseDomain {

    /**
     * 旧密码（加密）
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String oldPassword;

    /**
     * 新密码（加密）
     */
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String newPassword;

    /**
     * 昵称
     */
    @Size(max = 32, message = "昵称不能超过32个字符")
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    @SensitiveData(type = SensitiveType.MOBILE, scenes = SensitiveScene.LOG)
    private String mobile;

    /**
     * 邮箱
     */
    @SensitiveData(type = SensitiveType.EMAIL, scenes = SensitiveScene.LOG)
    private String email;

    /**
     * 性别：0-未知 1-男性 2-女性
     */
    @EnumValue(enumClass = GenderEnum.class)
    private String gender;
}
