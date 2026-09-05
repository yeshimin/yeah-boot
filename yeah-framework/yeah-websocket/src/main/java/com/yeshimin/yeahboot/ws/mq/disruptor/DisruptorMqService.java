package com.yeshimin.yeahboot.ws.mq.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.yeshimin.yeahboot.ws.mq.MqService;
import com.yeshimin.yeahboot.ws.mq.TopicConsumer;
import com.yeshimin.yeahboot.ws.websocket.domain.SessionKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class DisruptorMqService implements MqService {

    private DefaultEventProducer producer;

    @Autowired
    private List<TopicConsumer> consumers;

    @PostConstruct
    public void init() {
        DefaultEventFactory factory = new DefaultEventFactory();
        int bufferSize = 1024;
        Disruptor<DefaultEvent> disruptor = new Disruptor<>(factory, bufferSize, DaemonThreadFactory.INSTANCE);
        EventHandlerGroup<DefaultEvent> eventHandlerGroup = disruptor.handleEventsWith(new DefaultEventHandler());
        if (consumers != null && !consumers.isEmpty()) {
            for (TopicConsumer consumer : consumers) {
                eventHandlerGroup.then(new DefaultEventHandler(consumer));
            }
        }
        disruptor.start();

        RingBuffer<DefaultEvent> ringBuffer = disruptor.getRingBuffer();
        producer = new DefaultEventProducer(ringBuffer);
    }

    @Override
    public void publish(String topic, String message, SessionKey sk) {
        assert topic != null && message != null && sk != null;
        DefaultEvent event = new DefaultEvent();
        event.setTopic(topic);
        event.setData(message);
        event.setSk(sk);
        producer.onData(event);
    }
}
