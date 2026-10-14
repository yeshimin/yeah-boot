package com.yeshimin.yeahboot.api.encryption;

import com.yeshimin.yeahboot.common.service.CacheService;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiEncryptionServiceTest {

    private final SecureRandom random = new SecureRandom();
    private CacheService cacheService;
    private ApiEncryptionService service;

    @BeforeEach
    void setUp() {
        Security.addProvider(new BouncyCastleProvider());
        ApiEncryptionProperties properties = new ApiEncryptionProperties();
        properties.setMode(ApiEncryptionMode.REQUIRED);
        properties.setSuite(Sm2Sm4GcmApiEncryptionSuite.ID);
        properties.setKeyId("test-key");
        properties.setPrivateKey(this.createPrivateKey());

        cacheService = mock(CacheService.class);
        when(cacheService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        ReplayProtectionService replayProtectionService = new ReplayProtectionService(cacheService, properties);
        ApiEncryptionSuiteRegistry suiteRegistry = new ApiEncryptionSuiteRegistry(
                Collections.singletonList(new Sm2Sm4GcmApiEncryptionSuite()));
        service = new ApiEncryptionService(properties, replayProtectionService, suiteRegistry);
        service.init();
    }

    @Test
    void decryptsVerifiedEnvelopeAndConsumesNonce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        byte[] expected = "payload-ok".getBytes(StandardCharsets.UTF_8);

        byte[] actual = service.decryptRequest(this.encrypt(expected, request), request);

        assertArrayEquals(expected, actual);
        verify(cacheService).setIfAbsent(anyString(), anyString(), any());
    }

    @Test
    void rejectsModifiedTagBeforeConsumingNonce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        ApiEncryptionEnvelope envelope = this.encrypt("payload-ok".getBytes(StandardCharsets.UTF_8), request);
        envelope.setTag(envelope.getTag().substring(1) + "A");

        assertThrows(ApiEncryptionException.class, () -> service.decryptRequest(envelope, request));
        verify(cacheService, never()).setIfAbsent(anyString(), anyString(), any());
    }

    @Test
    void rejectsMismatchedPathBeforeConsumingNonce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        ApiEncryptionEnvelope envelope = this.encrypt("payload-ok".getBytes(StandardCharsets.UTF_8), request);
        envelope.setP("/admin/other");

        assertThrows(ApiEncryptionException.class, () -> service.decryptRequest(envelope, request));
        verify(cacheService, never()).setIfAbsent(anyString(), anyString(), any());
    }

    @Test
    void rejectsReplayedEnvelope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        ApiEncryptionEnvelope envelope = this.encrypt("payload-ok".getBytes(StandardCharsets.UTF_8), request);
        when(cacheService.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        assertThrows(ApiEncryptionException.class, () -> service.decryptRequest(envelope, request));
    }

    @Test
    void createsEncryptedResponseEnvelope() {
        ApiEncryptionResponseEnvelope envelope = service.encryptResponse(
                "response-ok".getBytes(StandardCharsets.UTF_8),
                new ApiEncryptionContext(new byte[16], "response-aad"));

        assertEquals(1, envelope.getV());
        assertEquals("test-key", envelope.getKid());
        assertNotNull(envelope.getIv());
        assertNotNull(envelope.getCt());
        assertNotNull(envelope.getTag());
    }

    private ApiEncryptionEnvelope encrypt(byte[] plaintext, MockHttpServletRequest request) throws Exception {
        byte[] baseKey = new byte[16];
        byte[] nonce = new byte[16];
        byte[] iv = new byte[12];
        random.nextBytes(baseKey);
        random.nextBytes(nonce);
        random.nextBytes(iv);

        ApiEncryptionPublicKeyVo publicKey = service.getPublicKey();
        long timestamp = System.currentTimeMillis() / 1000;
        String nonceValue = this.encode(nonce);
        String aad = "1|test-key|" + timestamp + '|' + nonceValue + "|POST|/admin/example|request";
        byte[] requestKey = this.deriveKey(baseKey, nonce, "request");

        Cipher cipher = Cipher.getInstance("SM4/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requestKey, "SM4"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(plaintext);

        ApiEncryptionEnvelope envelope = new ApiEncryptionEnvelope();
        envelope.setV(1);
        envelope.setKid("test-key");
        envelope.setTs(timestamp);
        envelope.setN(nonceValue);
        envelope.setP("/admin/example");
        envelope.setEk(this.encode(this.encryptSm2(baseKey, publicKey.getPublicKey())));
        envelope.setIv(this.encode(iv));
        envelope.setCt(this.encode(Arrays.copyOf(encrypted, encrypted.length - 16)));
        envelope.setTag(this.encode(Arrays.copyOfRange(encrypted, encrypted.length - 16, encrypted.length)));
        return envelope;
    }

    private byte[] encryptSm2(byte[] value, String publicKeyHex) throws Exception {
        X9ECParameters curve = GMNamedCurves.getByName("sm2p256v1");
        ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN());
        ECPublicKeyParameters publicKey = new ECPublicKeyParameters(
                curve.getCurve().decodePoint(Hex.decode(publicKeyHex)), domain);
        SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        engine.init(true, new ParametersWithRandom(publicKey, random));
        return engine.processBlock(value, 0, value.length);
    }

    private byte[] deriveKey(byte[] baseKey, byte[] nonce, String direction) throws Exception {
        Mac mac = Mac.getInstance("HMACSM3", BouncyCastleProvider.PROVIDER_NAME);
        mac.init(new SecretKeySpec(nonce, "HMACSM3"));
        byte[] prk = mac.doFinal(baseKey);
        mac.init(new SecretKeySpec(prk, "HMACSM3"));
        byte[] info = ("api-encryption:v1:" + direction).getBytes(StandardCharsets.UTF_8);
        byte[] input = Arrays.copyOf(info, info.length + 1);
        input[input.length - 1] = 1;
        return Arrays.copyOf(mac.doFinal(input), 16);
    }

    private String createPrivateKey() {
        X9ECParameters curve = GMNamedCurves.getByName("sm2p256v1");
        BigInteger value = new BigInteger(256, random).mod(curve.getN().subtract(BigInteger.ONE)).add(BigInteger.ONE);
        return value.toString(16);
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
