package com.yeshimin.yeahboot.mq.redis;

import com.yeshimin.yeahboot.mq.MqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisMqConfiguration {

    @Bean
    public RedisMqPublisher redisMqPublisher(StringRedisTemplate redisTemplate) {
        return new RedisMqPublisher(redisTemplate);
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer(
            RedisConnectionFactory factory, MqProperties properties) {
        StreamMessageListenerContainer.
                StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(properties.getBatchSize())
                        .pollTimeout(properties.getBlockTimeout())
                        .build();
        return StreamMessageListenerContainer.create(factory, options);
    }
}
