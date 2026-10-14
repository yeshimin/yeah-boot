package com.yeshimin.yeahboot.api.encryption;

import com.yeshimin.yeahboot.common.common.utils.ClientIpUtils;
import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * 防止已认证且成功解密的报文 Envelope 被重放。
 */
@Service
@RequiredArgsConstructor
public class ReplayProtectionService {

    /** 已认证 nonce 的 Redis 键前缀。 */
    private static final String CACHE_KEY_PREFIX = "api:encryption:replay:";

    /** 支持原子写入短期重放记录的缓存服务。 */
    private final CacheService cacheService;

    /** 提供防重放有效期的报文加密配置。 */
    private final ApiEncryptionProperties properties;

    /**
     * 原子登记已验证的 nonce；同一作用域和有效期内再次出现时拒绝请求。
     */
    public void register(HttpServletRequest request, String nonce) {
        // 对 Redis 键输入求摘要，避免暴露用户标识、IP 与 nonce 原文。
        String scope = this.resolveScope(request);
        String cacheKey = CACHE_KEY_PREFIX + this.sha256(scope + '|' + nonce);
        Boolean accepted = cacheService.setIfAbsent(cacheKey, "1",
                Duration.ofSeconds(properties.getReplayTtlSeconds()));
        if (!Boolean.TRUE.equals(accepted)) {
            throw new ApiEncryptionException("加密请求已失效或重复提交");
        }
    }

    /**
     * 已认证请求按用户隔离，匿名请求按真实 IP 和路径隔离。
     */
    private String resolveScope(HttpServletRequest request) {
        Long userId = WebContextUtils.getUserId();
        if (userId != null) {
            return "user:" + userId;
        }
        return "ip:" + ClientIpUtils.getClientIp(request) + ':' + request.getRequestURI();
    }

    /**
     * 对缓存键输入求摘要，避免将用户信息和 nonce 明文写入 Redis 键名。
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
