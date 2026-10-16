package com.sep.vox.application.response.input.assessmentitemresponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswer;
import com.sep.vox.domain.model.assessment.AssessmentItemAnswerTurn;

public record AssessmentItemAnswerDetailsResponse(
    UUID id,
    UUID sessionId,
    UUID paperItemId,
    String audioUrl,
    Integer durationSeconds,
    String transcript,
    Instant submittedAt,
    List<AssessmentItemAnswerTurnResponse> turns
) {

    public static AssessmentItemAnswerDetailsResponse toResponse(
            AssessmentItemAnswer answer,
            List<AssessmentItemAnswerTurn> turns) {
        return new AssessmentItemAnswerDetailsResponse(
            answer.getId(),
            answer.getSessionId(),
            answer.getPaperItemId(),
            answer.getAudioUrl(),
            answer.getDurationSeconds(),
            answer.getTranscript(),
            answer.getSubmittedAt(),
            turns.stream().map(AssessmentItemAnswerTurnResponse::toResponse).toList()
        );
    }
}
