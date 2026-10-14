package com.yeshimin.yeahboot.api.encryption;

/**
 * 服务端单个 API 加密套件的实现接口。
 *
 * <p>套件负责密码原语和密钥处理；协议服务负责 Envelope 校验、防重放、MVC 集成和响应生命周期。</p>
 */
public interface ApiEncryptionSuite {

    /** @return 部署配置中使用的唯一加密套件标识 */
    String getId();

    /**
     * 使用部署配置初始化密钥材料。
     *
     * @param properties 当前 API 加密配置
     */
    void initialize(ApiEncryptionProperties properties);

    /** @return 可安全公开给客户端的加密公钥 */
    String getPublicKey();

    /** @return 请求基础对称密钥长度，单位：字节 */
    int getBaseKeyLength();

    /** @return 请求随机 nonce 长度，单位：字节 */
    int getNonceLength();

    /** @return AEAD 初始化向量长度，单位：字节 */
    int getIvLength();

    /** @return AEAD 认证标签长度，单位：字节 */
    int getTagLength();

    /**
     * 解封客户端通过服务端公钥加密的请求基础密钥。
     *
     * @param encryptedKey 非对称加密后的请求基础密钥
     * @return 明文请求基础密钥
     */
    byte[] unwrapRequestKey(byte[] encryptedKey) throws Exception;

    /**
     * 从基础密钥和请求 nonce 派生请求或响应方向的工作密钥。
     *
     * @param baseKey 请求基础密钥
     * @param nonce   请求随机 nonce
     * @param direction 派生方向，例如 {@code request} 或 {@code response}
     * @return 派生后的工作密钥
     */
    byte[] deriveWorkingKey(byte[] baseKey, byte[] nonce, String direction) throws Exception;

    /**
     * 解密并验证带认证标签的密文。
     *
     * @param key        工作密钥
     * @param iv         初始化向量
     * @param ciphertext 业务密文
     * @param tag        认证标签
     * @param aad        认证附加数据
     * @return 明文数据
     */
    byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext, byte[] tag, String aad) throws Exception;

    /**
     * 加密明文并生成认证标签。
     *
     * @param key       工作密钥
     * @param iv        初始化向量
     * @param plaintext 明文数据
     * @param aad       认证附加数据
     * @return 业务密文和认证标签
     */
    ApiEncryptionCiphertext encrypt(byte[] key, byte[] iv, byte[] plaintext, String aad) throws Exception;
}
