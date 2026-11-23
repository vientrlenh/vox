package com.sep.vox.application.port.input.command.assessmentitemresponse;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.TurnType;

public record UpdateAssessmentItemResponseTurnCommand(
    UUID answerId,
    int turnOrder,
    TurnType turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    Instant answeredAt
) {
}
