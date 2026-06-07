package com.sep.vox.infrastructure.event.publisher;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ExternalEventKafkaValueSerializer implements Serializer<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (Exception exception) {
            throw new SerializationException("Khong the serialize external event cho topic " + topic, exception);
        }
    }
}
