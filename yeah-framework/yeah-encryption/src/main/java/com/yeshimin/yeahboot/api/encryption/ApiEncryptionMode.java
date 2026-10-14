package com.yeshimin.yeahboot.api.encryption;

/**
 * 全局报文加密模式。
 */
public enum ApiEncryptionMode {

    /** 不解析或输出加密 Envelope。 */
    OFF,

    /** 灰度期间同时接受明文和加密 Envelope。 */
    OPTIONAL,

    /** 除接口显式豁免外，全部要求使用加密 Envelope。 */
    REQUIRED
}
