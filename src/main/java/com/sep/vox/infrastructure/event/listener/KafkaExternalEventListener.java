package com.sep.vox.infrastructure.event.listener;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep.vox.application.port.input.ExternalEventHandler;

@Component
public class KafkaExternalEventListener implements MessageListener<String, JsonNode> {

    private static final Logger log = LoggerFactory.getLogger(KafkaExternalEventListener.class);

    private final List<ExternalEventHandler> handlers;

    public KafkaExternalEventListener(List<ExternalEventHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void onMessage(ConsumerRecord<String, JsonNode> record) {
        String eventType = record.key();
        JsonNode payload = record.value();

        log.info("Nhan external event: type={}, topic={}, partition={}, offset={}",
            eventType, record.topic(), record.partition(), record.offset());

        for (ExternalEventHandler handler : handlers) {
            try {
                handler.handle(eventType, payload);
            } catch (Exception exception) {
                log.error("Loi khi xu ly external event type={}: {}", eventType, exception.getMessage(), exception);
            }
        }
    }
}
