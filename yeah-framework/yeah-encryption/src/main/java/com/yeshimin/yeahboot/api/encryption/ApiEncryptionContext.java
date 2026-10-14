package com.yeshimin.yeahboot.api.encryption;

/**
 * 请求级加密上下文，仅保留至响应写出完成。
 */
public final class ApiEncryptionContext {

    /** 用于保存响应加密上下文的 Servlet 请求属性名。 */
    public static final String REQUEST_ATTRIBUTE = ApiEncryptionContext.class.getName();

    /** 响应方向的派生对称密钥，响应写出后会清零。 */
    private final byte[] responseKey;

    /** 响应认证附加数据。 */
    private final String responseAad;

    /**
     * 创建当前请求的响应加密上下文。
     *
     * @param responseKey 响应方向的派生对称密钥
     * @param responseAad 响应认证附加数据
     */
    public ApiEncryptionContext(byte[] responseKey, String responseAad) {
        this.responseKey = responseKey;
        this.responseAad = responseAad;
    }

    /**
     * 取得响应方向的派生对称密钥。
     *
     * @return 响应方向密钥
     */
    public byte[] responseKey() {
        return responseKey;
    }

    /**
     * 取得响应认证附加数据。
     *
     * @return 响应认证附加数据
     */
    public String responseAad() {
        return responseAad;
    }
}
