package com.yeshimin.yeahboot.common.common.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.id-generator")
public class IdGeneratorProperties {

    @PostConstruct
    public void init() {
        log.info("init [yeah-boot.id-generator] properties..." + "alphabet: {}, minLength: {}", "******", minLength);
    }

    private String alphabet;

    private Integer minLength;
}
