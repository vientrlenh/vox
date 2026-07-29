package com.sep.vox.interfaces.kafka.dto;

public record RecordingPartChangedEventDto(
    String eventId,
    String streamId,
    String scheduleId,
    String sessionId,
    String participantId,
    String streamType,
    String source,
    String status,
    String objectKey,
    Long durationSecs,
    boolean hasGaps,
    String errorMessage,
    String occurredAt
) {
}
