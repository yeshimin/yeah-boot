package com.yeshimin.yeahboot.api.encryption;

/**
 * 认证加密输出，由业务密文和认证标签组成。
 */
public final class ApiEncryptionCiphertext {

    /** 不包含认证标签的业务密文。 */
    private final byte[] ciphertext;

    /** 认证标签。 */
    private final byte[] tag;

    /**
     * 创建认证加密输出。
     *
     * @param ciphertext 不包含认证标签的业务密文
     * @param tag        认证标签
     */
    public ApiEncryptionCiphertext(byte[] ciphertext, byte[] tag) {
        this.ciphertext = ciphertext;
        this.tag = tag;
    }

    /**
     * 取得不包含认证标签的业务密文。
     *
     * @return 业务密文
     */
    public byte[] ciphertext() {
        return ciphertext;
    }

    /**
     * 取得认证标签。
     *
     * @return 认证标签
     */
    public byte[] tag() {
        return tag;
    }
}
