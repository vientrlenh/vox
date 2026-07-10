package com.sep.vox.interfaces.kafka.dto;

import java.util.List;

public record AnswerTurnsRecordedEventDto(
    String eventType,
    Integer schemaVersion,
    String answerId,
    AnswerTurnsRecordedPayloadDto payload
) {
    public record AnswerTurnsRecordedPayloadDto(
        List<AnswerTurnPayloadDto> turns,
        String reason
    ) {
    }

    public record AnswerTurnPayloadDto(
        String answerId,
        String sessionId,
        String paperItemId,
        Integer turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        Integer durationSeconds,
        Integer wordCount,
        String answeredAt
    ) {
    }
}
