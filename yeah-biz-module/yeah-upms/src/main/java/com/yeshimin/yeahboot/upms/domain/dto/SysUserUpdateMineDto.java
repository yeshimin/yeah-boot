package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.enums.GenderEnum;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserUpdateMineDto extends BaseDomain {

    /**
     * 旧密码（加密）
     */
    private String oldPassword;

    /**
     * 新密码（加密）
     */
    private String newPassword;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别：1-男性 2-女性
     */
    @EnumValue(enumClass = GenderEnum.class)
    private String gender;
}
