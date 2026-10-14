package com.yeshimin.yeahboot.api.encryption;

import lombok.Data;

/**
 * 加密响应 Envelope。
 */
@Data
public class ApiEncryptionResponseEnvelope {

    /** 协议版本。 */
    private int v;

    /** 服务端用于响应加密的密钥标识。 */
    private String kid;

    /** Base64URL 编码的 SM4-GCM 初始化向量。 */
    private String iv;

    /** Base64URL 编码的 SM4-GCM 响应业务密文。 */
    private String ct;

    /** Base64URL 编码的 SM4-GCM 128 位认证标签。 */
    private String tag;
}
