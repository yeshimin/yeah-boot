package com.yeshimin.yeahboot.api.encryption;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;

/**
 * 默认的 SM2 密钥包装、SM3-HKDF 派生和 SM4-GCM 加密套件。
 */
@Component
public class Sm2Sm4GcmApiEncryptionSuite implements ApiEncryptionSuite {

    /** 当前内置 SM2 + SM4-GCM 密码套件的唯一标识。 */
    public static final String ID = "sm2-sm4-gcm-v1";

    /** Bouncy Castle 安全提供者名称。 */
    private static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    /** SM4-GCM JCA 算法名称。 */
    private static final String SM4_GCM = "SM4/GCM/NoPadding";

    /** SM3 HMAC JCA 算法名称。 */
    private static final String HMAC_SM3 = "HMACSM3";

    /** 每次请求随机生成的 SM4 基础密钥长度，单位：字节。 */
    private static final int BASE_KEY_LENGTH = 16;

    /** 请求随机 nonce 长度，单位：字节。 */
    private static final int NONCE_LENGTH = 16;

    /** SM4-GCM 初始化向量长度，单位：字节。 */
    private static final int IV_LENGTH = 12;

    /** SM4-GCM 认证标签长度，单位：字节。 */
    private static final int TAG_LENGTH = 16;

    /** 当前部署注入的 SM2 私钥及由其推导的公开公钥。 */
    private volatile KeyMaterial keyMaterial;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void initialize(ApiEncryptionProperties properties) {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (!StringUtils.hasText(properties.getPrivateKey())) {
            throw new IllegalStateException("api.encryption.private-key must be configured for " + ID);
        }
        keyMaterial = this.createKeyMaterial(properties.getPrivateKey());
    }

    @Override
    public String getPublicKey() {
        return Hex.toHexString(this.getKeyMaterial().publicKey);
    }

    @Override
    public int getBaseKeyLength() {
        return BASE_KEY_LENGTH;
    }

    @Override
    public int getNonceLength() {
        return NONCE_LENGTH;
    }

    @Override
    public int getIvLength() {
        return IV_LENGTH;
    }

    @Override
    public int getTagLength() {
        return TAG_LENGTH;
    }

    @Override
    public byte[] unwrapRequestKey(byte[] encryptedKey) throws Exception {
        SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        engine.init(false, this.getKeyMaterial().privateKey);
        return engine.processBlock(encryptedKey, 0, encryptedKey.length);
    }

    @Override
    public byte[] deriveWorkingKey(byte[] baseKey, byte[] nonce, String direction) throws Exception {
        byte[] prk = this.hmac(nonce, baseKey);
        try {
            byte[] info = ("api-encryption:v1:" + direction).getBytes(StandardCharsets.UTF_8);
            byte[] input = Arrays.copyOf(info, info.length + 1);
            input[input.length - 1] = 1;
            return Arrays.copyOf(this.hmac(prk, input), BASE_KEY_LENGTH);
        } finally {
            Arrays.fill(prk, (byte) 0);
        }
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext, byte[] tag, String aad) throws Exception {
        Cipher cipher = Cipher.getInstance(SM4_GCM, PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SM4"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(this.concat(ciphertext, tag));
    }

    @Override
    public ApiEncryptionCiphertext encrypt(byte[] key, byte[] iv, byte[] plaintext, String aad) throws Exception {
        Cipher cipher = Cipher.getInstance(SM4_GCM, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "SM4"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        byte[] output = cipher.doFinal(plaintext);
        int ciphertextLength = output.length - TAG_LENGTH;
        return new ApiEncryptionCiphertext(Arrays.copyOf(output, ciphertextLength),
                Arrays.copyOfRange(output, ciphertextLength, output.length));
    }

    /**
     * 使用 SM3 HMAC 计算 HKDF 所需的中间值。
     */
    private byte[] hmac(byte[] key, byte[] input) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SM3, PROVIDER);
        mac.init(new SecretKeySpec(key, HMAC_SM3));
        return mac.doFinal(input);
    }

    /**
     * 从十六进制 SM2 私钥派生服务端私钥对象和可公开的非压缩公钥。
     */
    private KeyMaterial createKeyMaterial(String privateKeyHex) {
        try {
            String normalizedPrivateKey = privateKeyHex.replaceFirst("^(0x|0X)", "");
            if (normalizedPrivateKey.length() % 2 != 0) {
                normalizedPrivateKey = '0' + normalizedPrivateKey;
            }
            byte[] privateKeyBytes = Hex.decode(normalizedPrivateKey);
            BigInteger privateValue = new BigInteger(1, privateKeyBytes);
            X9ECParameters curve = GMNamedCurves.getByName("sm2p256v1");
            ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN());
            if (privateValue.signum() <= 0 || privateValue.compareTo(domain.getN()) >= 0) {
                throw new IllegalArgumentException("invalid SM2 private key");
            }
            ECPrivateKeyParameters privateKey = new ECPrivateKeyParameters(privateValue, domain);
            byte[] publicKey = domain.getG().multiply(privateValue).normalize().getEncoded(false);
            return new KeyMaterial(privateKey, publicKey);
        } catch (Exception e) {
            throw new IllegalStateException("invalid api.encryption.private-key", e);
        }
    }

    /**
     * 取得已初始化的 SM2 密钥材料。
     */
    private KeyMaterial getKeyMaterial() {
        KeyMaterial material = keyMaterial;
        if (material == null) {
            throw new IllegalStateException("API encryption suite is not initialized");
        }
        return material;
    }

    /**
     * 拼接 GCM 业务密文和认证标签，供 JCA 解密器校验。
     */
    private byte[] concat(byte[] first, byte[] second) {
        byte[] merged = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    /**
     * 当前 SM2 密钥材料，仅保留在进程内存中。
     */
    private static final class KeyMaterial {

        /** 用于解封请求基础密钥的 SM2 私钥参数。 */
        private final ECPrivateKeyParameters privateKey;

        /** 由私钥推导的非压缩 SM2 公钥字节。 */
        private final byte[] publicKey;

        /**
         * 创建当前部署的 SM2 密钥材料。
         *
         * @param privateKey SM2 私钥参数
         * @param publicKey  非压缩 SM2 公钥字节
         */
        private KeyMaterial(ECPrivateKeyParameters privateKey, byte[] publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
    }
}
