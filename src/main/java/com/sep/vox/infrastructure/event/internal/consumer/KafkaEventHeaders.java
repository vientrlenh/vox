package com.sep.vox.infrastructure.event.internal.consumer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public final class KafkaEventHeaders {

    private static final String HEADER_EVENT_ID = "eventId";
    private static final String HEADER_EVENT_TYPE = "eventType";

    public static UUID readEventId(ConsumerRecord<String, String> record) {
        var eventId = readEventIdOrNull(record);
        if (eventId == null) {
            throw new IllegalStateException("Thiếu hoặc sai header '" + HEADER_EVENT_ID + "'  tại offset " + record.offset());
        }
        return eventId;
    }

    /**
     * Nhiều eventType dùng chung một topic (mail phúc khảo, vòng đời điểm, phân công
     * chấm), nên consumer phải đọc header này để biết parse payload thành record nào.
     */
    public static String readEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(HEADER_EVENT_TYPE);
        if (header == null || header.value() == null) {
            throw new IllegalStateException(
                "Thiếu header '" + HEADER_EVENT_TYPE + "' tại offset " + record.offset());
        }
        var eventType = new String(header.value(), StandardCharsets.UTF_8).trim();
        if (eventType.isEmpty()) {
            throw new IllegalStateException(
                "Header '" + HEADER_EVENT_TYPE + "' rỗng tại offset " + record.offset());
        }
        return eventType;
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
