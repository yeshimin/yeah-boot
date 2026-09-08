package com.yeshimin.yeahboot.common.common.enums;

import lombok.Getter;

/**
 * 系统参数定义
 */
@Getter
public enum SysConfigEnum {

    /**
     * 是否启用管理后台登录验证码
     */
    CAPTCHA_ENABLED("yeah-boot.captcha-enabled", SysConfigValueTypeEnum.BOOLEAN, "true"),

    /**
     * 管理后台登录失败统计窗口，单位：秒
     */
    ADMIN_LOGIN_FAILURE_WINDOW_SECONDS(
            "auth.admin-login.failure-window-seconds", SysConfigValueTypeEnum.LONG, "600"),

    /**
     * 管理后台最大登录失败次数
     */
    ADMIN_LOGIN_MAX_FAILURE_COUNT(
            "auth.admin-login.max-failure-count", SysConfigValueTypeEnum.LONG, "5"),

    /**
     * 管理后台登录锁定时间，单位：秒
     */
    ADMIN_LOGIN_LOCK_SECONDS(
            "auth.admin-login.lock-seconds", SysConfigValueTypeEnum.LONG, "600"),

    /**
     * 短信验证码长度
     */
    SMS_CODE_LENGTH("yeah-boot.sms-code-length", SysConfigValueTypeEnum.INTEGER, "6"),

    /**
     * 短信验证码有效期，单位：秒
     */
    SMS_CODE_EXP_SECONDS("yeah-boot.sms-code-exp-seconds", SysConfigValueTypeEnum.INTEGER, "300"),

    /**
     * 阿里云短信模板编号
     */
    SMS_TEMPLATE_CODE("yeah-boot.notification.aliyun.sms.template-code", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 阿里云短信签名
     */
    SMS_SIGN_NAME("yeah-boot.notification.aliyun.sms.sign-name", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 用户导入文件最大大小，单位：MB
     */
    SYS_USER_EXCEL_MAX_IMPORT_FILE_SIZE_MB(
            "yeah-boot.sys-user-excel.max-import-file-size-mb", SysConfigValueTypeEnum.LONG, "5"),

    /**
     * 单次最大用户导入条数
     */
    SYS_USER_EXCEL_MAX_IMPORT_ROWS(
            "yeah-boot.sys-user-excel.max-import-rows", SysConfigValueTypeEnum.INTEGER, "1000"),

    /**
     * 单次最大用户导出条数
     */
    SYS_USER_EXCEL_MAX_EXPORT_ROWS(
            "yeah-boot.sys-user-excel.max-export-rows", SysConfigValueTypeEnum.INTEGER, "10000"),

    /**
     * 用户导入错误最大展示数量
     */
    SYS_USER_EXCEL_MAX_ERROR_MESSAGES(
            "yeah-boot.sys-user-excel.max-error-messages", SysConfigValueTypeEnum.INTEGER, "20");

    private final String key;
    private final SysConfigValueTypeEnum valueType;
    private final String defaultValue;

    SysConfigEnum(String key, SysConfigValueTypeEnum valueType, String defaultValue) {
        this.key = key;
        this.valueType = valueType;
        this.defaultValue = defaultValue;
    }

    public static SysConfigEnum ofKey(String key) {
        for (SysConfigEnum config : values()) {
            if (config.getKey().equals(key)) {
                return config;
            }
        }
        return null;
    }
}
