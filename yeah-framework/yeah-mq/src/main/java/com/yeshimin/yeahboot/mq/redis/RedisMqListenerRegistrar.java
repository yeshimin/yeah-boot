package com.yeshimin.yeahboot.mq.redis;

import com.yeshimin.yeahboot.mq.MqListener;
import com.yeshimin.yeahboot.mq.MqProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class RedisMqListenerRegistrar {

    @Autowired
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MqProperties properties;

    @Autowired(required = false)
    private List<MqListener> listeners;

    @PostConstruct
    public void register() {
        for (MqListener listener : listeners) {
            String stream = listener.getTopic();

            ensureConsumerGroup(stream);

            // 用适配器包装业务 listener
            RedisMqListener adapter = new RedisMqListener(listener, redisTemplate, properties);

            container.receive(
                    Consumer.from(properties.getDefaultGroup(), UUID.randomUUID().toString()),
                    StreamOffset.create(stream, ReadOffset.lastConsumed()),
                    adapter
            );
        }

        container.start();
    }

    /**
     * 确保 topic 对应的 Stream 与消费组已创建。
     */
    private void ensureConsumerGroup(String stream) {
        // 最后一个参数对应 Redis 的 MKSTREAM，不产生初始化业务消息。
        try {
            redisTemplate.execute((RedisCallback<String>) connection -> connection.xGroupCreate(
                    stream.getBytes(StandardCharsets.UTF_8),
                    properties.getDefaultGroup(),
                    ReadOffset.latest(),
                    true));
        } catch (Exception ignored) {
            // 消费组已存在时 Redis 会抛出 BUSYGROUP，保持原有组即可。
        }
    }

    @PreDestroy
    public void stop() {
        container.stop();
    }
}
