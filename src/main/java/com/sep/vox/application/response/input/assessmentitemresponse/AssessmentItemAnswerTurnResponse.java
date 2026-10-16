package com.sep.vox.application.response.input.assessmentitemresponse;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswerTurn;
import com.sep.vox.domain.model.assessment.AssessmentTurnType;

public record AssessmentItemAnswerTurnResponse(
    UUID id,
    UUID assessmentItemAnswerId,
    int turnOrder,
    AssessmentTurnType turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    Instant answeredAt,
    Instant createdAt
) {

    public static AssessmentItemAnswerTurnResponse toResponse(AssessmentItemAnswerTurn turn) {
        return new AssessmentItemAnswerTurnResponse(
            turn.getId(),
            turn.getAssessmentItemAnswerId(),
            turn.getTurnOrder(),
            turn.getTurnType(),
            turn.getPromptText(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            turn.getDurationSeconds(),
            turn.getWordCount(),
            turn.getAnsweredAt(),
            turn.getCreatedAt()
        );
    }
}
