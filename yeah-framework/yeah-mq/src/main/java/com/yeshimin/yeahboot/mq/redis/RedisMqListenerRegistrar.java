package com.yeshimin.yeahboot.mq.redis;

import com.yeshimin.yeahboot.mq.MqListener;
import com.yeshimin.yeahboot.mq.MqProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

            // 创建消费组（如果不存在）
            try {
                redisTemplate.opsForStream().createGroup(stream, properties.getDefaultGroup());
            } catch (Exception ignored) {
            }

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
}
