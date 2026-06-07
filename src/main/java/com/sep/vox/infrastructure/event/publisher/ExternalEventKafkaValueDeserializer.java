package com.sep.vox.infrastructure.event.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class ExternalEventKafkaValueDeserializer implements Deserializer<JsonNode> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public JsonNode deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (Exception exception) {
            throw new SerializationException("Khong the deserialize external event tu topic " + topic, exception);
        }
    }
}
