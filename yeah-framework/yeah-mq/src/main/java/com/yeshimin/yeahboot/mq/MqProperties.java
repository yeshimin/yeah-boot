package com.yeshimin.yeahboot.mq;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.mq")
public class MqProperties {

    @PostConstruct
    private void init() {
        log.info("init [yeah-boot.mq] properties...this: {}", this);
    }

    // redis | rabbitmq
    private String impl = "redis";

    private boolean autoAck = true;

    private String defaultGroup = "DEFAULT_GROUP";

    private int batchSize = 10;

    private Duration blockTimeout = Duration.ofSeconds(2);
}
