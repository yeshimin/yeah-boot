package com.yeshimin.yeahboot.api.encryption;

/**
 * 报文加密共用的 Servlet 请求属性和协议头名称。
 */
public final class ApiEncryptionAttributes {

    /** 标识请求或响应采用 API 加密协议的 HTTP Header 名称。 */
    public static final String MARKER_HEADER = "Api-Encryption";

    /** 当前加密协议标识 Header 的版本值。 */
    public static final String MARKER_VALUE_V1 = "v1";

    /** 承载 Base64URL 编码加密 Envelope 的唯一查询参数名。 */
    public static final String QUERY_PARAMETER = "__enc";

    /** 标记当前请求已完成解密的 Servlet 属性名。 */
    public static final String ENCRYPTED_ATTRIBUTE = ApiEncryptionAttributes.class.getName() + ".encrypted";

    /** 标记当前接口是否按策略要求加密的 Servlet 属性名。 */
    public static final String EXPECTED_ATTRIBUTE = ApiEncryptionAttributes.class.getName() + ".expected";

    private ApiEncryptionAttributes() {
    }
}
