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
    ADMIN_AUTH_LOGIN_CAPTCHA_ENABLED(
            "admin.auth.login.captcha-enabled", SysConfigValueTypeEnum.BOOLEAN, "true"),

    /**
     * 是否显示管理后台登录页公告
     */
    ADMIN_AUTH_LOGIN_NOTICE_ENABLED(
            "admin.auth.login.notice.enabled", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台登录页公告标题
     */
    ADMIN_AUTH_LOGIN_NOTICE_TITLE(
            "admin.auth.login.notice.title", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 管理后台登录页公告内容
     */
    ADMIN_AUTH_LOGIN_NOTICE_CONTENT(
            "admin.auth.login.notice.content", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 是否开放管理后台自注册
     */
    ADMIN_AUTH_LOGIN_REGISTER_ENABLED(
            "admin.auth.login.register.enabled", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台自注册默认角色编码
     */
    ADMIN_AUTH_LOGIN_REGISTER_DEFAULT_ROLE_CODE(
            "admin.auth.login.register.default-role-code", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 管理后台登录失败统计窗口，单位：秒
     */
    ADMIN_AUTH_LOGIN_FAILURE_WINDOW_SECONDS(
            "admin.auth.login.failure-window-seconds", SysConfigValueTypeEnum.LONG, "600"),

    /**
     * 管理后台最大登录失败次数
     */
    ADMIN_AUTH_LOGIN_FAILURE_COUNT(
            "admin.auth.login.failure-count", SysConfigValueTypeEnum.LONG, "5"),

    /**
     * 管理后台登录锁定时间，单位：秒
     */
    ADMIN_AUTH_LOGIN_LOCK_SECONDS(
            "admin.auth.login.lock-seconds", SysConfigValueTypeEnum.LONG, "600"),

    /**
     * 是否按客户端IP区分管理后台登录失败次数
     */
    ADMIN_AUTH_LOGIN_FAILURE_INCLUDE_IP_ENABLED(
            "admin.auth.login.failure.include-ip.enabled", SysConfigValueTypeEnum.BOOLEAN, "true"),

    /**
     * 是否启用管理后台登录IP白名单
     */
    ADMIN_AUTH_LOGIN_IP_WHITELIST_ENABLED(
            "admin.auth.login.ip-whitelist.enabled", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台登录IP白名单，多个IP使用逗号分隔
     */
    ADMIN_AUTH_LOGIN_IP_WHITELIST_VALUES(
            "admin.auth.login.ip-whitelist.values", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 管理后台密码最小长度，用于前端密码规则提示
     */
    ADMIN_AUTH_PASSWORD_MIN_LENGTH(
            "admin.auth.password.min-length", SysConfigValueTypeEnum.INTEGER, "6"),

    /**
     * 管理后台密码最大长度，用于前端密码规则提示
     */
    ADMIN_AUTH_PASSWORD_MAX_LENGTH(
            "admin.auth.password.max-length", SysConfigValueTypeEnum.INTEGER, "32"),

    /**
     * 管理后台密码是否要求大写字母，用于前端密码规则提示
     */
    ADMIN_AUTH_PASSWORD_REQUIRE_UPPERCASE(
            "admin.auth.password.require-uppercase", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台密码是否要求小写字母，用于前端密码规则提示
     */
    ADMIN_AUTH_PASSWORD_REQUIRE_LOWERCASE(
            "admin.auth.password.require-lowercase", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台密码是否要求数字，用于前端密码规则提示
     */
    ADMIN_AUTH_PASSWORD_REQUIRE_DIGIT(
            "admin.auth.password.require-digit", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台密码是否要求特殊字符，用于前端密码规则提示
     */
    ADMIN_AUTH_PASSWORD_REQUIRE_SPECIAL(
            "admin.auth.password.require-special", SysConfigValueTypeEnum.BOOLEAN, "false"),

    /**
     * 管理后台文件上传最大大小，单位：MB
     */
    ADMIN_STORAGE_UPLOAD_MAX_SIZE_MB(
            "admin.storage.upload.max-size-mb", SysConfigValueTypeEnum.LONG, "10"),

    /**
     * 是否启用管理后台上传文件类型校验
     */
    ADMIN_STORAGE_UPLOAD_VALIDATION_ENABLED(
            "admin.storage.upload.validation.enabled", SysConfigValueTypeEnum.BOOLEAN, "true"),

    /**
     * 管理后台文件上传允许扩展名，多个值使用逗号分隔
     */
    ADMIN_STORAGE_UPLOAD_ALLOWED_EXTENSIONS(
            "admin.storage.upload.allowed-extensions", SysConfigValueTypeEnum.STRING,
            "jpg,jpeg,png,webp,pdf,txt,doc,docx,xls,xlsx,zip"),

    /**
     * 管理后台文件上传允许MIME类型，多个值使用逗号分隔
     */
    ADMIN_STORAGE_UPLOAD_ALLOWED_MIME_TYPES(
            "admin.storage.upload.allowed-mime-types", SysConfigValueTypeEnum.STRING,
            "image/jpeg,image/png,image/webp,application/pdf,text/plain,application/msword,"
                    + "application/vnd.openxmlformats-officedocument.wordprocessingml.document,"
                    + "application/vnd.ms-excel,"
                    + "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,"
                    + "application/zip,application/x-zip-compressed"),

    /**
     * 是否允许管理后台上传公开预览文件
     */
    ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ENABLED(
            "admin.storage.upload.public-preview.enabled", SysConfigValueTypeEnum.BOOLEAN, "true"),

    /**
     * 管理后台允许公开预览的文件扩展名，多个值使用逗号分隔
     */
    ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ALLOWED_EXTENSIONS(
            "admin.storage.upload.public-preview.allowed-extensions", SysConfigValueTypeEnum.STRING,
            "jpg,jpeg,png,webp"),

    /**
     * 全局短信验证码长度
     */
    GLOBAL_SMS_CODE_LENGTH("global.sms.code-length", SysConfigValueTypeEnum.INTEGER, "6"),

    /**
     * 全局短信验证码有效期，单位：秒
     */
    GLOBAL_SMS_CODE_EXP_SECONDS("global.sms.code-exp-seconds", SysConfigValueTypeEnum.INTEGER, "300"),

    /**
     * 全局阿里云短信模板编号
     */
    GLOBAL_SMS_ALIYUN_TEMPLATE_CODE("global.sms.aliyun.template-code", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 全局阿里云短信签名
     */
    GLOBAL_SMS_ALIYUN_SIGN_NAME("global.sms.aliyun.sign-name", SysConfigValueTypeEnum.STRING, ""),

    /**
     * 管理后台用户导入文件最大大小，单位：MB
     */
    ADMIN_IMPORT_EXPORT_SYS_USER_EXCEL_MAX_IMPORT_FILE_SIZE_MB(
            "admin.import-export.sys-user-excel.max-import-file-size-mb", SysConfigValueTypeEnum.LONG, "5"),

    /**
     * 管理后台单次最大用户导入条数
     */
    ADMIN_IMPORT_EXPORT_SYS_USER_EXCEL_MAX_IMPORT_ROWS(
            "admin.import-export.sys-user-excel.max-import-rows", SysConfigValueTypeEnum.INTEGER, "1000"),

    /**
     * 管理后台单次最大用户导出条数
     */
    ADMIN_IMPORT_EXPORT_SYS_USER_EXCEL_MAX_EXPORT_ROWS(
            "admin.import-export.sys-user-excel.max-export-rows", SysConfigValueTypeEnum.INTEGER, "10000"),

    /**
     * 管理后台用户导入错误最大展示数量
     */
    ADMIN_IMPORT_EXPORT_SYS_USER_EXCEL_MAX_ERROR_MESSAGES(
            "admin.import-export.sys-user-excel.max-error-messages", SysConfigValueTypeEnum.INTEGER, "20");

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
