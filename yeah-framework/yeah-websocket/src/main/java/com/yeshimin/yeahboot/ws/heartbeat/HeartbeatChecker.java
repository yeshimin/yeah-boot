package com.yeshimin.yeahboot.ws.heartbeat;

import com.yeshimin.yeahboot.ws.websocket.config.WebSocketProperties;
import com.yeshimin.yeahboot.ws.websocket.holder.DefaultSessionHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "yeah-boot.websocket.heartbeat", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class HeartbeatChecker {

    private final WebSocketProperties webSocketProperties;

    private final DefaultSessionHolder sessionHolder;

    private static final ScheduledExecutorService CHECKER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "HeartbeatChecker");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    public void init() {
        log.info("HeartbeatChecker initialized: heartbeat checking is enabled.");

        long interval = webSocketProperties.getHeartbeat().getInterval();
        CHECKER.scheduleAtFixedRate(this::clean, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * 清理心跳超时的ws链接
     */
    private void clean() {
        long now = System.currentTimeMillis();

        try {
            log.debug("[HeartbeatChecker] 开始清理... now={}", now);
            sessionHolder.cleanHeartbeatTimeout(webSocketProperties.getHeartbeat().getTimeout());
        } catch (Exception e) {
            log.error("[HeartbeatChecker] 清理异常", e);
        }
    }
}
