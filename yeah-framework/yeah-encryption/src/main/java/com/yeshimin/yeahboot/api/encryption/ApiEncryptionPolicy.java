package com.yeshimin.yeahboot.api.encryption;

/**
 * 接口级报文加密策略。
 */
public enum ApiEncryptionPolicy {

    /** 跟随全局模式。 */
    DEFAULT,

    /** 当前接口必须使用加密 Envelope。 */
    REQUIRED,

    /** 当前接口在迁移期间同时接受明文和加密 Envelope。 */
    OPTIONAL,

    /** 当前接口显式保持明文。 */
    BYPASS
}
