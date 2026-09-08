package com.yeshimin.yeahboot.ws.websocket.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.websocket")
public class WebSocketProperties {

    @PostConstruct
    private void init() {
        log.info("init [yeah-boot.websocket] properties...heartbeatEnabled: {}, heartbeatInterval: {}, "
                        + "heartbeatTimeout: {}",
                heartbeat == null ? null : heartbeat.getEnabled(),
                heartbeat == null ? null : heartbeat.getInterval(),
                heartbeat == null ? null : heartbeat.getTimeout());
    }

    private Heartbeat heartbeat;

    @Data
    public static class Heartbeat {
        // 是否启用心跳检测
        private Boolean enabled;
        // 心跳检测间隔（秒）
        private Integer interval;
        // 心跳检测超时时间（秒）
        private Integer timeout;
    }
}
