package com.yeshimin.yeahboot.api.encryption;

import lombok.Data;

/**
 * 加密请求 Envelope。
 */
@Data
public class ApiEncryptionEnvelope {

    /** 协议版本。 */
    private Integer v;

    /** 服务端加密密钥标识，用于密钥轮换。 */
    private String kid;

    /** 客户端请求时间戳，Unix 时间戳，单位秒。 */
    private Long ts;

    /** Base64URL 编码的 16 字节随机 nonce，用于派生密钥与防重放。 */
    private String n;

    /** 请求 URI 路径，不包含查询参数；参与 AAD 完整性校验。 */
    private String p;

    /** Base64URL 编码的 SM2 密文，内容为本次请求随机生成的 SM4 基础密钥。 */
    private String ek;

    /** Base64URL 编码的 SM4-GCM 初始化向量。 */
    private String iv;

    /** Base64URL 编码的 SM4-GCM 业务密文。 */
    private String ct;

    /** Base64URL 编码的 SM4-GCM 128 位认证标签。 */
    private String tag;
}
