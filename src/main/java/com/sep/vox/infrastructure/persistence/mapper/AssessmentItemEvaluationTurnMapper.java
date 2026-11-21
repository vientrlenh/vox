package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemEvaluationTurnJpaEntity;

public final class AssessmentItemEvaluationTurnMapper {

    private AssessmentItemEvaluationTurnMapper() {
    }

    public static AssessmentItemEvaluationTurn toDomain(AssessmentItemEvaluationTurnJpaEntity jpa) {
        return AssessmentItemEvaluationTurn.builder()
                .id(jpa.getId())
                .evaluationId(jpa.getEvaluationId())
                .turnOrder(jpa.getTurnOrder())
                .turnType(TurnType.valueOf(jpa.getTurnType()))
                .promptText(jpa.getPromptText())
                .audioUrl(jpa.getAudioUrl())
                .transcript(jpa.getTranscript())
                .wordCount(jpa.getWordCount())
                .durationSeconds(jpa.getDurationSeconds())
                .asrConfidence(jpa.getAsrConfidence())
                .pronunciationOverallJson(jpa.getPronunciationOverall())
                .wordFeedbackJson(jpa.getWordFeedback())
                .build();
    }

    public static AssessmentItemEvaluationTurnJpaEntity toJpa(AssessmentItemEvaluationTurn domain) {
        return AssessmentItemEvaluationTurnJpaEntity.builder()
                .id(domain.getId())
                .evaluationId(domain.getEvaluationId())
                .turnOrder(domain.getTurnOrder())
                .turnType(domain.getTurnType().name())
                .promptText(domain.getPromptText())
                .audioUrl(domain.getAudioUrl())
                .transcript(domain.getTranscript())
                .wordCount(domain.getWordCount())
                .durationSeconds(domain.getDurationSeconds())
                .asrConfidence(domain.getAsrConfidence())
                .pronunciationOverall(domain.getPronunciationOverallJson())
                .wordFeedback(domain.getWordFeedbackJson())
                .build();
    }
}
