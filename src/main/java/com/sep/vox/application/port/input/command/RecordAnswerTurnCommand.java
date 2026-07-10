package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecordAnswerTurnCommand(
    UUID answerId,
    UUID sessionId,
    UUID paperItemId,
    int turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    OffsetDateTime answeredAt
) {
}
