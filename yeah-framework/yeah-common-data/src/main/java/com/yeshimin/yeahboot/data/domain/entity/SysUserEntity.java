package com.yeshimin.yeahboot.data.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserEntity extends ConditionBaseEntity<SysUserEntity> {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密）
     */
    @JsonIgnore
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    /**
     * 状态：1-启用 2-禁用
     */
    private String status;

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
    private Integer gender;

    /**
     * 备注
     */
    private String remark;
}
