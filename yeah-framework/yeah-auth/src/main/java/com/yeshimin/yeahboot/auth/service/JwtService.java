package com.yeshimin.yeahboot.auth.service;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson2.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.yeshimin.yeahboot.auth.common.properties.AuthTokenProperties;
import com.yeshimin.yeahboot.auth.common.properties.JwtProperties;
import com.yeshimin.yeahboot.auth.domain.vo.JwtPayloadVo;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Jwt服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final AuthTokenProperties authTokenProperties;

    // 签发算法
    private Algorithm algorithm;
    // map<subject-terminal, Algorithm>
    private final Map<String, Algorithm> algorithms = new HashMap<>();
    // map<subject, jwt config>
    private final Map<String, JwtProperties> jwtConfigs = new HashMap<>();

    @PostConstruct
    private void init() {
        // 初始化算法 HMAC 默认全局配置
        algorithm = Algorithm.HMAC256(jwtProperties.getSecret());

        // 主题
        if (authTokenProperties.getSubjects() != null) {
            for (AuthTokenProperties.Subject subject : authTokenProperties.getSubjects()) {
                // 终端
                if (subject.getTerminals() != null) {
                    for (AuthTokenProperties.Terminal terminal : subject.getTerminals()) {
                        // 签发算法
                        String key = subject.getName() + "-" + terminal.getName();
                        // 如果subject.jwt未配置，则使用全局jwt配置
                        String secret = subject.getJwt() != null && subject.getJwt().getSecret() != null ?
                                subject.getJwt().getSecret() : jwtProperties.getSecret();
                        algorithms.put(key, Algorithm.HMAC256(secret));
                    }
                }

                // jwt配置
                if (subject.getJwt() != null) {
                    // 如果jwt配置不为空，则使用该配置
                    jwtConfigs.put(subject.getName(), subject.getJwt());
                } else {
                    // 否则使用全局jwt配置
                    jwtConfigs.put(subject.getName(), jwtProperties);
                }
            }
        }

        log.info("init [JwtService]...algorithms: {}", algorithms);
    }

    // ================================================================================

    /**
     * 签发Jwt
     */
    public String signJwt(String userId, String subject, String terminal) {
        JwtProperties jwtProp = jwtConfigs.get(subject);

        Date now = new Date();
        // expireSeconds类型已改为Long，避免溢出
        Date exp = new Date(now.getTime() + jwtProp.getExpireSeconds() * 1000);
        JWTCreator.Builder builder = JWT.create()
                // 受众（用户标识）
                .withAudience(userId)
                // 签发时间
                .withIssuedAt(now)
                // 主题：区分（子）系统/模块
                .withSubject(subject)
                // 过期时间
//                .withExpiresAt(exp)  // 由缓存过期机制控制
                ;

        // 终端
        if (terminal != null) {
            builder.withClaim(CommonConsts.JWT_CLAIM_TERMINAL, terminal);
        }
        // 毫秒时间
        builder.withClaim(CommonConsts.JWT_CLAIM_IAT_MS, now.getTime());
        builder.withClaim(CommonConsts.JWT_CLAIM_EXP_MS, exp.getTime());

        // 获取算法
        Algorithm algorithm = algorithms.get(subject + "-" + terminal);
        if (algorithm == null) {
            algorithm = this.algorithm;
        }

        // 签发算法
        return builder.sign(algorithm);
    }

    /**
     * 解码Jwt
     */
    public DecodedJWT decodeJwt(String jwt, String subject, String terminal) {
        JwtProperties jwtProp = jwtConfigs.get(subject);

        // 获取算法
        Algorithm algorithm = algorithms.get(subject + "-" + terminal);
        if (algorithm == null) {
            algorithm = this.algorithm;
        }

        JWTVerifier verifier = JWT.require(algorithm)
                // 时间校验偏差
                .acceptLeeway(jwtProp.getDefaultLeeway())
                .build();

        try {
            return verifier.verify(jwt);
        } catch (JWTDecodeException e) {
            log.error("jwt: {} valid fail", jwt);
        } catch (Exception e) {
            log.error("jwt: {} valid error", jwt);
        }
        return null;
    }

    public JwtPayloadVo decodePayload(String jwt) {
        DecodedJWT decodedJWT = JWT.decode(jwt);

        String payload = Base64.decodeStr(decodedJWT.getPayload());
        JwtPayloadVo payloadVo = JSON.parseObject(payload, JwtPayloadVo.class);
        if (payloadVo == null) {
            throw new IllegalArgumentException("jwt is invalid");
        }
        payloadVo.setExp((int) (payloadVo.getExpMs() / 1000));
        return payloadVo;
    }

    public Long getExpireSeconds(String subject) {
        return jwtConfigs.get(subject).getExpireSeconds();
    }
}
