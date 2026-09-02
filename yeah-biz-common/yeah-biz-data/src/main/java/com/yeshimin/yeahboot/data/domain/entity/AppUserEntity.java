package com.yeshimin.yeahboot.data.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import com.yeshimin.yeahboot.common.domain.base.ConditionBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * app端用户表
 */
@Schema(description = "app端用户表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user")
public class AppUserEntity extends ConditionBaseEntity<AppUserEntity> {

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    @SensitiveData(type = SensitiveType.MOBILE, scenes = SensitiveScene.LOG)
    private String mobile;

    /**
     * 登录密码
     */
    @JsonIgnore
    @Schema(description = "登录密码")
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    /**
     * 账号
     */
    @Schema(description = "账号")
    private String account;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 性别
     */
    @Schema(description = "性别")
    private Integer gender;

    /**
     * 生日
     */
    @Schema(description = "生日")
    private LocalDate birthday;
}
