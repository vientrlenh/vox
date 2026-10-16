package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemEvaluationTurnJpaEntity;

public final class AssessmentItemEvaluationTurnMapper {

    private AssessmentItemEvaluationTurnMapper() {
    }

    public static AssessmentItemEvaluationTurn toDomain(AssessmentItemEvaluationTurnJpaEntity jpa) {
        return new AssessmentItemEvaluationTurn(
            jpa.getId(),
            jpa.getEvaluationId(),
            jpa.getTurnOrder(),
            TurnType.valueOf(jpa.getTurnType()),
            jpa.getPromptText(),
            jpa.getAudioUrl(),
            jpa.getTranscript(),
            jpa.getWordCount(),
            jpa.getDurationSeconds(),
            jpa.getAsrConfidence(),
            jpa.getPronunciationOverall(),
            jpa.getWordFeedback()
        );
    }

    public static AssessmentItemEvaluationTurnJpaEntity toJpa(AssessmentItemEvaluationTurn domain) {
        return new AssessmentItemEvaluationTurnJpaEntity(
            domain.getId(),
            domain.getEvaluationId(),
            domain.getTurnOrder(),
            domain.getTurnType().name(),
            domain.getPromptText(),
            domain.getAudioUrl(),
            domain.getTranscript(),
            domain.getWordCount(),
            domain.getDurationSeconds(),
            domain.getAsrConfidence(),
            domain.getPronunciationOverallJson(),
            domain.getWordFeedbackJson()
        );
    }
}
