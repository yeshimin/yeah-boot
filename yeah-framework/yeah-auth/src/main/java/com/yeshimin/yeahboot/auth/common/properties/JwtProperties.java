package com.yeshimin.yeahboot.auth.common.properties;

import com.alibaba.fastjson2.annotation.JSONField;
import com.yeshimin.yeahboot.common.common.serialize.DefaultDesensitizeSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.token.jwt")
public class JwtProperties {

    @PostConstruct
    private void init() {
        log.info("init [auth.token.jwt] properties...secret: {}, expireSeconds: {}, defaultLeeway: {}",
                "******", expireSeconds, defaultLeeway);
    }

    /**
     * 签发密钥
     */
    @JSONField(serializeUsing = DefaultDesensitizeSerializer.class)
    private String secret;

    /**
     * 过期时间（秒）
     * 避免乘以1000后超出Integer范围，改为Long类型
     */
    private Long expireSeconds;

    /**
     * 时间校验偏差（秒）
     * 这里顺便一起改掉，同上
     */
    private Long defaultLeeway;
}
