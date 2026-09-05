package com.yeshimin.yeahboot.ws.mq.disruptor;

import com.lmax.disruptor.EventHandler;
import com.yeshimin.yeahboot.ws.mq.TopicConsumer;

import java.util.Objects;

public class DefaultEventHandler implements EventHandler<DefaultEvent> {

    private final TopicConsumer topicConsumer;

    public DefaultEventHandler() {
        this(null);
    }

    public DefaultEventHandler(TopicConsumer topicConsumer) {
        this.topicConsumer = topicConsumer;
    }

    @Override
    public void onEvent(DefaultEvent event, long sequence, boolean endOfBatch) {
        if (topicConsumer == null) {
            return;
        }

        if (Objects.equals(event.getTopic(), topicConsumer.getTopic())) {
            topicConsumer.consume(topicConsumer.getTopic(), event.getData(), event.getSk());
        }
    }
}
