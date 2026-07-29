package com.sep.vox.infrastructure.event.internal.consumer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public final class KafkaEventHeaders {

    private static final String HEADER_EVENT_ID = "eventId";

    public static UUID readEventId(ConsumerRecord<String, String> record) {
        var eventId = readEventIdOrNull(record);
        if (eventId == null) {
            throw new IllegalStateException("Thiếu hoặc sai header '" + HEADER_EVENT_ID + "'  tại offset " + record.offset());
        }
        return eventId;
    }

    public static UUID readEventIdOrNull(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(HEADER_EVENT_ID);
        if (header == null || header.value() == null) {
            return null;
        }
        try {
            return UUID.fromString(new String(header.value(), StandardCharsets.UTF_8).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
