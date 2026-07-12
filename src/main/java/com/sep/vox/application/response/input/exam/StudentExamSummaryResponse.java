package com.sep.vox.application.response.input.exam;

import java.util.UUID;

public record StudentExamSummaryResponse(
    UUID id,
    String title,
    String subject,
    String description,
    int duration,
    String examDate,
    String status,
    String kind,
    UUID sessionId,
    Integer maxAttempt,
    int attemptsUsed,
    boolean canEnter,
    String entryMessage
) {
}
