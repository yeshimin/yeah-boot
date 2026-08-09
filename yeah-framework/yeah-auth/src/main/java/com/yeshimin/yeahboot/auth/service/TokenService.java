package com.yeshimin.yeahboot.auth.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yeshimin.yeahboot.auth.domain.vo.JwtPayloadVo;
import com.yeshimin.yeahboot.common.common.consts.CacheKeyConsts;
import com.yeshimin.yeahboot.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Token相关
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final CacheService cacheService;

    /**
     * 生成token
     */
    public String generateToken(String userId, String subject, String terminal) {
        return jwtService.signJwt(userId, subject, terminal);
    }

    /**
     * 解析token
     */
    public JwtPayloadVo decodeToken(String token) {
        JwtPayloadVo payloadVo = jwtService.decodePayload(token);
        DecodedJWT decodedJWT = jwtService.decodeJwt(token, payloadVo.getSub(), payloadVo.getTerm());
        return decodedJWT != null ? payloadVo : null;
    }

    /**
     * 缓存token
     */
    public void cacheToken(String subject, String userId, String terminal, String token, Long timestampMs) {
        // 区分是否有终端
        final String key = String.format(CacheKeyConsts.USER_TERMINAL_TOKEN, subject, userId, terminal, timestampMs);
        cacheService.set(key, token, jwtService.getExpireSeconds(subject));
    }

    /**
     * 获取缓存的token
     */
    public String getCacheToken(String subject, String userId, String terminal, Long iatMs) {
        // 有终端的
        return cacheService.get(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN, subject, userId, terminal, iatMs));
    }

    /**
     * 删除缓存的token
     */
    public void deleteToken(String subject, String userId, String terminal, Long iatMs) {
        // 删除token
        cacheService.delete(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN, subject, userId, terminal, iatMs));
        // 删除terminal下token信息
        cacheService.deleteHashFields(
                String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subject, userId, terminal),
                String.valueOf(iatMs));
    }

    /**
     * 删除指定用户在指定主体下的所有token
     */
    public void deleteUserTokens(String subject, String userId) {
        // 获取指定subject的信息
        Map<String, String> subjectTerminalInfo = this.getSubjectTerminalInfo(subject, userId);
        // 遍历subject下的terminal
        subjectTerminalInfo.keySet().forEach(terminal -> {
            // 获取terminal信息
            Map<String, String> terminalTokenInfo = this.getTerminalTokenInfo(subject, userId, terminal);
            // 删除terminal下的所有token
            terminalTokenInfo.keySet()
                    .forEach(iatMs -> this.deleteToken(subject, userId, terminal, Long.valueOf(iatMs)));
            // 删除terminal信息
            cacheService.delete(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subject, userId, terminal));
        });
        // 删除subject信息
        cacheService.delete(String.format(CacheKeyConsts.USER_SUBJECT_TERMINAL_INFO, subject, userId));
    }

    /**
     * 获取sub下用户所有终端信息
     * hash类型
     */
    public Map<String, String> getSubjectTerminalInfo(String subject, String userId) {
        Map<Object, Object> map =
                cacheService.getHash(String.format(CacheKeyConsts.USER_SUBJECT_TERMINAL_INFO, subject, userId));
        // convert to map<String, String>
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));
    }

    /**
     * 获取终端下token信息
     * hash类型
     */
    public Map<String, String> getTerminalTokenInfo(String subject, String userId, String terminal) {
        Map<Object, Object> map =
                cacheService.getHash(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subject, userId, terminal));
        // convert to map<String, String>
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));
    }

    /**
     * 设置sub下用户终端信息
     */
    public void setSubjectTerminalInfo(String subject, String userId, String terminal, Long iatMs, Long expMs) {
        String key = String.format(CacheKeyConsts.USER_SUBJECT_TERMINAL_INFO, subject, userId);
        if (cacheService.exists(key)) {
            cacheService.setHash(key, terminal, iatMs + "," + expMs);
        } else {
            Map<String, String> map = new HashMap<>();
            map.put(terminal, iatMs + "," + expMs);
            cacheService.setHash(key, map);
        }
        Long expire = cacheService.getExpire(key);
        Long configExpireSeconds = jwtService.getExpireSeconds(subject);
        cacheService.expire(key, expire == null || configExpireSeconds > expire ? configExpireSeconds : expire);
    }

    /**
     * 设置终端下token信息
     */
    public void setTerminalTokenInfo(String subject, String userId, String terminal, Long iatMs, Long expMs) {
        String key = String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subject, userId, terminal);
        if (cacheService.exists(key)) {
            cacheService.setHash(key, String.valueOf(iatMs), String.valueOf(expMs));
        } else {
            Map<String, String> map = new HashMap<>();
            map.put(String.valueOf(iatMs), String.valueOf(expMs));
            cacheService.setHash(key, map);
        }
        Long expire = cacheService.getExpire(key);
        Long configExpireSeconds = jwtService.getExpireSeconds(subject);
        cacheService.expire(key, expire == null || configExpireSeconds > expire ? configExpireSeconds : expire);
    }

    /**
     * 刷新token过期时间（以及token信息和对应终端信息）
     * NOTE 如何操作频率太高出现性能问题，可降低刷新频率，比如过期时间在10分钟以内才刷新
     */
    public void refreshTokenExpire(String subject, String userId, String terminal, Long iatMs) {
        Long configExpireSeconds = jwtService.getExpireSeconds(subject);
        Long expireMs = System.currentTimeMillis() + configExpireSeconds * 1000;

        // 刷新token过期时间
        String tokenKey = String.format(CacheKeyConsts.USER_TERMINAL_TOKEN, subject, userId, terminal, iatMs);
        cacheService.expire(tokenKey, configExpireSeconds);

        // 刷新token信息过期时间
        String tokenInfoKey = String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subject, userId, terminal);
        cacheService.setHash(tokenInfoKey, String.valueOf(iatMs), String.valueOf(expireMs));
        cacheService.expire(tokenInfoKey, configExpireSeconds);

        // 刷新终端信息过期时间
        String terminalInfoKey = String.format(CacheKeyConsts.USER_SUBJECT_TERMINAL_INFO, subject, userId);
        cacheService.setHash(terminalInfoKey, terminal, iatMs + "," + expireMs);
        cacheService.expire(terminalInfoKey, configExpireSeconds);
    }
}
