package com.sep.vox.application.mapper.assessmentitemresponse;

import java.util.List;

import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;

public final class AssessmentItemResponseResponseMapper {

    private AssessmentItemResponseResponseMapper() {
    }

    public static AssessmentItemResponseDetailsResponse toDetailsResponse(
            AssessmentItemResponse response,
            List<AssessmentItemResponseTurn> turns) {
        return new AssessmentItemResponseDetailsResponse(
            response.getId(),
            response.getSessionId(),
            response.getPaperItemId(),
            response.getAudioUrl(),
            response.getDurationSeconds(),
            response.getTranscript(),
            response.getSubmittedAt() == null ? null : response.getSubmittedAt().toString(),
            turns.stream().map(AssessmentItemResponseResponseMapper::toTurnResponse).toList()
        );
    }

    public static AssessmentItemResponseTurnResponse toTurnResponse(AssessmentItemResponseTurn turn) {
        return new AssessmentItemResponseTurnResponse(
            turn.getId(),
            turn.getAssessmentItemResponseId(),
            turn.getTurnOrder(),
            turn.getTurnType().name(),
            turn.getPromptText(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            turn.getDurationSeconds(),
            turn.getWordCount(),
            turn.getAnsweredAt() == null ? null : turn.getAnsweredAt().toString(),
            turn.getCreatedAt() == null ? null : turn.getCreatedAt().toString()
        );
    }
}
