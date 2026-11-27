package com.sep.vox.infrastructure.event.internal.consumer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

public final class KafkaEventHeaders {

    private static final String HEADER_EVENT_ID = "eventId";
    private static final String HEADER_EVENT_TYPE = "eventType";

    public static UUID readEventId(ConsumerRecord<String, String> record) {
        UUID eventId = readEventIdOrNull(record);
        if (eventId == null) {
            throw new IllegalStateException("Missing or invalid header '" + HEADER_EVENT_ID + "'  at offset " + record.offset());
        }
        return eventId;
    }

    /**
     * Several eventTypes share one topic (appeal mail, result lifecycle, grading
     * assignment), so the consumer must read this header to know which record to parse the payload into.
     */
    public static String readEventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(HEADER_EVENT_TYPE);
        if (header == null || header.value() == null) {
            throw new IllegalStateException(
                "Missing header '" + HEADER_EVENT_TYPE + "' at offset " + record.offset());
        }
        String eventType = new String(header.value(), StandardCharsets.UTF_8).trim();
        if (eventType.isEmpty()) {
            throw new IllegalStateException(
                "Header '" + HEADER_EVENT_TYPE + "' is empty at offset " + record.offset());
        }
        return eventType;
    }

    public static UUID readEventIdOrNull(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(HEADER_EVENT_ID);
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
