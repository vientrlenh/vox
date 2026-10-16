package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswerTurn;
import com.sep.vox.domain.model.assessment.AssessmentTurnType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseTurnJpaEntity;

public final class AssessmentItemResponseTurnMapper {

    private AssessmentItemResponseTurnMapper() {
    }

    public static AssessmentItemAnswerTurn toDomain(AssessmentItemResponseTurnJpaEntity jpa) {
        return new AssessmentItemAnswerTurn(
            jpa.getId(),
            jpa.getAssessmentItemAnswerId(),
            jpa.getTurnOrder(),
            AssessmentTurnType.valueOf(jpa.getTurnType()),
            jpa.getPromptText(),
            jpa.getAudioUrl(),
            jpa.getTranscript(),
            jpa.getDurationSeconds(),
            jpa.getWordCount(),
            jpa.getAnsweredAt(),
            jpa.getCreatedAt()
        );
    }

    public static AssessmentItemResponseTurnJpaEntity toJpa(AssessmentItemAnswerTurn domain) {
        return new AssessmentItemResponseTurnJpaEntity(
            domain.getId(),
            domain.getAssessmentItemAnswerId(),
            domain.getTurnOrder(),
            domain.getTurnType().name(),
            domain.getPromptText(),
            domain.getAudioUrl(),
            domain.getTranscript(),
            domain.getDurationSeconds(),
            domain.getWordCount(),
            domain.getAnsweredAt(),
            domain.getCreatedAt()
        );
    }
}
