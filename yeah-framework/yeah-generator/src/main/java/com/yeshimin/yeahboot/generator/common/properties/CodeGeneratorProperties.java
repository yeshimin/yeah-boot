package com.yeshimin.yeahboot.generator.common.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.code-generator")
public class CodeGeneratorProperties {

    @PostConstruct
    public void init() {
        log.info("init [yeah-boot.code-generator] properties...url: {}, username: {}, password: {}, author: {}, "
                        + "pack: {}, module: {}, tablePrefix: {}, baseEntityFields: {}",
                "******", "******", "******", author, pack, module, tablePrefix, baseEntityFields);
    }

    private String url;

    private String username;

    private String password;

    private String author;

    private String pack;

    private String module;

    private List<String> tablePrefix;

    private List<String> baseEntityFields;
}
