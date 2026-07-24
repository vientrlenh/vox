package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamItemEvaluationTurn;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationTurnJpaEntity;

public final class ExamItemEvaluationTurnMapper {

    private ExamItemEvaluationTurnMapper() {
    }

    public static ExamItemEvaluationTurn toDomain(ExamItemEvaluationTurnJpaEntity jpa) {
        return new ExamItemEvaluationTurn(
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

    public static ExamItemEvaluationTurnJpaEntity toJpa(ExamItemEvaluationTurn domain) {
        return new ExamItemEvaluationTurnJpaEntity(
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
