package com.yeshimin.yeahboot.api.encryption;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * API 加密协议编排服务。
 *
 * <p>密码学实现由部署时选定的 {@link ApiEncryptionSuite} 负责；本服务负责通用 Envelope 协议、
 * 防重放、请求上下文和响应生命周期。</p>
 */
@Service
@RequiredArgsConstructor
public class ApiEncryptionService {

    /** 为每个加密响应生成独立初始化向量的安全随机数生成器。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 报文加密协议的部署配置。 */
    private final ApiEncryptionProperties properties;

    /** 成功认证后登记 nonce 的防重放服务。 */
    private final ReplayProtectionService replayProtectionService;

    /** 按部署配置定位实际密码套件的注册表。 */
    private final ApiEncryptionSuiteRegistry suiteRegistry;

    /** 已完成初始化的当前密码套件；协议关闭时保持为空。 */
    private volatile ApiEncryptionSuite suite;

    /**
     * 在协议启用时初始化部署选定的密码套件及其密钥材料。
     */
    @PostConstruct
    void init() {
        if (properties.isEnabled()) {
            ApiEncryptionSuite activeSuite = suiteRegistry.get(properties.getSuite());
            activeSuite.initialize(properties);
            suite = activeSuite;
        }
    }

    /**
     * 返回客户端创建加密 Envelope 所需的公开元数据。
     */
    public ApiEncryptionPublicKeyVo getPublicKey() {
        ApiEncryptionPublicKeyVo vo = new ApiEncryptionPublicKeyVo();
        vo.setEnabled(properties.isEnabled());
        vo.setRequired(properties.getMode() == ApiEncryptionMode.REQUIRED);
        vo.setVersion(properties.getVersion());
        vo.setSuite(properties.getSuite());
        vo.setKeyId(properties.getKeyId());
        vo.setTimestampWindowSeconds(properties.getTimestampWindowSeconds());
        if (properties.isEnabled()) {
            vo.setPublicKey(this.getSuite().getPublicKey());
        }
        return vo;
    }

    /**
     * 判断对象是否包含加密 Envelope 的协议版本字段。
     *
     * @param envelope 待判断的请求 Envelope
     * @return 是否为加密 Envelope
     */
    public boolean isEncryptedEnvelope(ApiEncryptionEnvelope envelope) {
        return envelope != null && envelope.getV() != null;
    }

    /**
     * 解密并认证传入的 Envelope，仅成功认证后才会占用 nonce。
     */
    public byte[] decryptRequest(ApiEncryptionEnvelope envelope, HttpServletRequest request) {
        ApiEncryptionSuite activeSuite = this.getSuite();
        this.validateEnvelope(envelope, request, activeSuite);
        byte[] baseKey = null;
        byte[] requestKey = null;
        try {
            baseKey = activeSuite.unwrapRequestKey(this.decode(envelope.getEk()));
            if (baseKey.length != activeSuite.getBaseKeyLength()) {
                throw new ApiEncryptionException("加密报文无效");
            }

            // 请求与响应使用同一基础密钥派生不同方向的工作密钥。
            byte[] nonce = this.decode(envelope.getN());
            requestKey = activeSuite.deriveWorkingKey(baseKey, nonce, "request");
            byte[] plaintext = activeSuite.decrypt(requestKey, this.decode(envelope.getIv()),
                    this.decode(envelope.getCt()), this.decode(envelope.getTag()),
                    this.requestAad(envelope, request));

            // 仅成功通过认证的 Envelope 可以占用 nonce。
            replayProtectionService.register(request, envelope.getN());

            byte[] responseKey = activeSuite.deriveWorkingKey(baseKey, nonce, "response");
            request.setAttribute(ApiEncryptionContext.REQUEST_ATTRIBUTE,
                    new ApiEncryptionContext(responseKey, this.responseAad(envelope, request)));
            return plaintext;
        } catch (ApiEncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiEncryptionException("加密报文无效");
        } finally {
            this.clear(baseKey);
            this.clear(requestKey);
        }
    }

    /**
     * 使用当前请求的响应方向密钥加密控制器响应。
     */
    public ApiEncryptionResponseEnvelope encryptResponse(byte[] plaintext, ApiEncryptionContext context) {
        ApiEncryptionSuite activeSuite = this.getSuite();
        byte[] iv = new byte[activeSuite.getIvLength()];
        RANDOM.nextBytes(iv);
        try {
            ApiEncryptionCiphertext ciphertext = activeSuite.encrypt(
                    context.responseKey(), iv, plaintext, context.responseAad());
            ApiEncryptionResponseEnvelope envelope = new ApiEncryptionResponseEnvelope();
            envelope.setV(properties.getVersion());
            envelope.setKid(properties.getKeyId());
            envelope.setIv(this.encode(iv));
            envelope.setCt(this.encode(ciphertext.ciphertext()));
            envelope.setTag(this.encode(ciphertext.tag()));
            return envelope;
        } catch (Exception e) {
            throw new IllegalStateException("响应加密失败", e);
        }
    }

    /**
     * 取得当前请求成功解密后建立的响应加密上下文。
     *
     * @param request 当前 Servlet 请求
     * @return 响应加密上下文；请求未加密或解密失败时返回 {@code null}
     */
    public ApiEncryptionContext getContext(HttpServletRequest request) {
        Object context = request.getAttribute(ApiEncryptionContext.REQUEST_ATTRIBUTE);
        return context instanceof ApiEncryptionContext ? (ApiEncryptionContext) context : null;
    }

    /**
     * 清零响应方向密钥并移除当前请求的加密上下文。
     *
     * @param request 当前 Servlet 请求
     */
    public void clearContext(HttpServletRequest request) {
        ApiEncryptionContext context = this.getContext(request);
        if (context != null) {
            this.clear(context.responseKey());
            request.removeAttribute(ApiEncryptionContext.REQUEST_ATTRIBUTE);
        }
    }

    private void validateEnvelope(ApiEncryptionEnvelope envelope, HttpServletRequest request,
                                  ApiEncryptionSuite activeSuite) {
        // 先校验协议字段完整性和当前服务端密钥标识，再进行密码学处理。
        if (!properties.isEnabled()
                || envelope == null
                || envelope.getV() == null
                || envelope.getV() != properties.getVersion()
                || !properties.getKeyId().equals(envelope.getKid())
                || !StringUtils.hasText(envelope.getN())
                || !StringUtils.hasText(envelope.getP())
                || !StringUtils.hasText(envelope.getEk())
                || !StringUtils.hasText(envelope.getIv())
                || !StringUtils.hasText(envelope.getCt())
                || !StringUtils.hasText(envelope.getTag())
                || envelope.getTs() == null) {
            throw new ApiEncryptionException("加密报文无效");
        }
        if (!request.getRequestURI().equals(envelope.getP())) {
            throw new ApiEncryptionException("加密报文无效");
        }

        // 时间窗口限制与 nonce 登记共同构成短期防重放边界。
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (Math.abs(nowSeconds - envelope.getTs()) > properties.getTimestampWindowSeconds()) {
            throw new ApiEncryptionException("加密请求已失效或重复提交");
        }

        byte[] nonce = this.decode(envelope.getN());
        byte[] iv = this.decode(envelope.getIv());
        byte[] encryptedKey = this.decode(envelope.getEk());
        byte[] ciphertext = this.decode(envelope.getCt());
        byte[] tag = this.decode(envelope.getTag());
        if (nonce.length != activeSuite.getNonceLength()
                || iv.length != activeSuite.getIvLength()
                || encryptedKey.length == 0
                || ciphertext.length > properties.getMaxPayloadBytes()
                || tag.length != activeSuite.getTagLength()) {
            throw new ApiEncryptionException("加密报文无效");
        }
    }

    /**
     * 取得已初始化的当前密码套件。
     */
    private ApiEncryptionSuite getSuite() {
        ApiEncryptionSuite activeSuite = suite;
        if (activeSuite == null) {
            throw new IllegalStateException("API encryption is not enabled");
        }
        return activeSuite;
    }

    /**
     * 构建请求方向 AEAD 认证附加数据。
     */
    private String requestAad(ApiEncryptionEnvelope envelope, HttpServletRequest request) {
        return this.baseAad(envelope, request) + "|request";
    }

    /**
     * 构建响应方向 AEAD 认证附加数据。
     */
    private String responseAad(ApiEncryptionEnvelope envelope, HttpServletRequest request) {
        return this.baseAad(envelope, request) + "|response";
    }

    /**
     * 将协议版本、密钥标识、时间戳、随机数、HTTP 方法和路径绑定到认证数据。
     */
    private String baseAad(ApiEncryptionEnvelope envelope, HttpServletRequest request) {
        return envelope.getV() + "|" + envelope.getKid() + "|" + envelope.getTs() + "|"
                + envelope.getN() + "|" + request.getMethod() + "|" + request.getRequestURI();
    }

    /**
     * 使用无填充 Base64URL 编码协议二进制字段。
     */
    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /**
     * 解码 Base64URL 协议字段，并将格式错误统一转换为协议异常。
     */
    private byte[] decode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new ApiEncryptionException("加密报文无效");
        }
    }

    /**
     * 主动覆盖短生命周期密钥字节，缩短其在堆内存中的可见时间。
     */
    private void clear(byte[] value) {
        if (value != null) {
            Arrays.fill(value, (byte) 0);
        }
    }
}
