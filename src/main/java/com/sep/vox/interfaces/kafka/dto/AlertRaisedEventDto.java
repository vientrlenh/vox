package com.sep.vox.interfaces.kafka.dto;

/**
 * The payload of the topic {@code assessment.alert.raised} published by vox-streaming (see domain.AlertRaisedEvent over there).
 *
 * <p>{@code participantId} is the candidateId, {@code sessionId} is the id of the assessment session. These two fields were once
 * assigned the same value on the publishing side; vox-streaming now looks the value up before publishing, but the old data
 * on the topic is still exactly as it was -- so the read side must not treat participantId as certain.
 */
public record AlertRaisedEventDto(
    String eventId,
    String raisedAt,
    String source,
    String sessionId,
    String participantId,
    String streamId,
    String streamType,
    String alertType,
    String detail,
    Double confidence,
    Long sequenceNo,
    String level,
    String capturedAt
) {
}
