package com.yeshimin.yeahboot.api.encryption;

import lombok.Data;

/**
 * 浏览器创建加密 Envelope 所需的公开元数据。
 */
@Data
public class ApiEncryptionPublicKeyVo {

    /** 是否启用了 API 报文加密。 */
    private boolean enabled;

    /** 当前服务是否要求所有未豁免接口必须使用加密报文。 */
    private boolean required;

    /** 当前 API 加密协议版本。 */
    private int version;

    /** 服务端当前启用的加密套件标识。 */
    private String suite;

    /** 服务端当前加密密钥标识，用于客户端选择和密钥轮换。 */
    private String keyId;

    /** 十六进制编码的 SM2 非压缩公钥，仅用于加密请求基础密钥。 */
    private String publicKey;

    /** 请求时间戳允许的最大偏差，单位：秒。 */
    private long timestampWindowSeconds;
}
