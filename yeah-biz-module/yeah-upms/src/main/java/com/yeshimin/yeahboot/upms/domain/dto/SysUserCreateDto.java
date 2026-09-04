package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.common.enums.GenderEnum;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import com.yeshimin.yeahboot.common.common.validation.EnumValue;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserCreateDto extends BaseDomain {

    /**
     * 组织ID集合
     */
    private Set<Long> orgIds;

    /**
     * 岗位ID集合
     */
    private Set<Long> postIds;

    /**
     * 角色ID集合
     */
    private Set<Long> roleIds;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度必须在2到32个字符之间")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    /**
     * 状态：1-启用 2-禁用
     */
    @EnumValue(enumClass = DataStatusEnum.class)
    private String status;

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

    /**
     * 备注
     */
    private String remark;
}
