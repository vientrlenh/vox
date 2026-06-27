package com.sep.vox.interfaces.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.interfaces.kafka.handler.dummy.DummyUserRegisteredExternalEventHandler;

import tools.jackson.databind.json.JsonMapper;


@Component
public class DummyUserEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyUserEventConsumer.class);

    private final DummyUserRegisteredExternalEventHandler userRegisteredHandler;
    private final JsonMapper jsonMapper;

    public DummyUserEventConsumer(
            DummyUserRegisteredExternalEventHandler userRegisteredHandler, 
            JsonMapper jsonMapper) {
        this.userRegisteredHandler = userRegisteredHandler;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4", 
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000), 
        autoCreateTopics = "true", 
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.user-service.topic.user-registered}", 
        groupId = "${app.external-event.kafka.consumer-groups.user-service.group-id}"
    )
    public void userRegisterConsume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            var eventKey = record.key();
            var payload = jsonMapper.valueToTree(record.value());
            LOGGER.info("Receive event: type={}, topic={}, partition={}, offset={}",
                eventKey, record.topic(), record.partition(), record.offset());
            userRegisteredHandler.handle(eventKey, payload);
            ack.acknowledge();
        } catch (Exception e) {
            LOGGER.error("User registered processed failed, no ack: {}", e.getMessage(), e);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("DLT message at topic {}: {}", record.topic(), record.value());
    }
}
