package com.yeshimin.yeahboot.ws.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.ws.mq.MqService;
import com.yeshimin.yeahboot.ws.mq.TopicConst;
import com.yeshimin.yeahboot.ws.mq.TopicConsumer;
import com.yeshimin.yeahboot.ws.mq.command.BaseCommand;
import com.yeshimin.yeahboot.ws.mq.command.BizCommandHandler;
import com.yeshimin.yeahboot.ws.websocket.domain.SessionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务处理主题消费者
 */
@Slf4j
@Component
public class BizHandleTopicConsumer implements TopicConsumer {

    @Lazy
    @Autowired
    private MqService mqService;

    @Autowired
    private List<BizCommandHandler> bizCommandHandlers;
    private Map<String, BizCommandHandler> bizCommandHandlerMap;

    @PostConstruct
    public void init() {
        // to map
        bizCommandHandlerMap = bizCommandHandlers.stream()
                .collect(Collectors.toMap(BizCommandHandler::getCommand, v -> v, (k1, k2) -> k1));
    }

    @Override
    public void consume(String topic, String message, SessionKey sk) {
        log.info("BizHandleTopicConsumer...topic: {}, sk: {}", topic, sk);

        BaseCommand command = JSON.parseObject(message, BaseCommand.class);

        BizCommandHandler bizCommandHandler = bizCommandHandlerMap.get(command.getCommand());
        if (bizCommandHandler == null) {
            log.warn("bizHandler...command unknown");
            return;
        }

        bizCommandHandler.handle(command, sk);
    }

    @Override
    public String getTopic() {
        return TopicConst.BIZ_HANDLE;
    }
}
