package com.yeshimin.yeahboot.auth.common.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.token")
public class AuthTokenProperties {

    private final Map<String, Subject> mapSubject = new HashMap<>();

    @PostConstruct
    private void init() {
        log.info("init [auth.token] properties...jwt: {}, subjects: {}, subjectCount: {}",
                "******", "******", subjects == null ? 0 : subjects.size());

        for (Subject subject : subjects) {
            // 检查配置是否正确
            if (subject.getMaxOnlineTerminalCount() == null) {
                throw new RuntimeException("subject.maxOnlineTerminalCount can not be null");
            }
            if (subject.getMaxOnlineTokenCount() == null) {
                throw new RuntimeException("subject.maxOnlineTokenCount can not be null");
            }

            if (subject.getTerminals() == null || subject.getTerminals().isEmpty()) {
                throw new RuntimeException("subject.terminals can not be empty");
            }
            for (Terminal terminal : subject.getTerminals()) {
                if (terminal.getName() == null || terminal.getName().isEmpty()) {
                    throw new RuntimeException("subject.terminals.name can not be empty");
                }
                if (terminal.getMaxOnlineTokenCount() == null) {
                    throw new RuntimeException("subject.terminals.maxOnlineTokenCount can not be null");
                }
            }

            // 保存subject到mapSubject
            mapSubject.put(subject.getName(), subject);

            // subject.terminals list to map
            for (Terminal terminal : subject.getTerminals()) {
                subject.getMapTerminal().put(terminal.getName(), terminal);
            }
        }
    }

    @NestedConfigurationProperty
    private JwtProperties jwt;
    private List<Subject> subjects;

    @Data
    public static class Subject {
        private String name;
        private String apiPrefix;
        private JwtProperties jwt;
        private List<Terminal> terminals;
        private Integer maxOnlineTokenCount;
        private Integer maxOnlineTerminalCount;

        private Map<String, Terminal> mapTerminal = new HashMap<>();
    }

    @Data
    public static class Terminal {
        private String name;
        private Integer maxOnlineTokenCount;
    }
}
