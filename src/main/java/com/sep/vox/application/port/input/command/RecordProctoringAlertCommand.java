package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecordProctoringAlertCommand(
    String eventId,
    UUID examSessionId,
    UUID candidateId,
    String streamId,
    String streamType,
    String alertType,
    String level,
    String source,
    String detail,
    BigDecimal confidence,
    Long sequenceNo,
    Instant capturedAt,
    Instant raisedAt
) {
}
