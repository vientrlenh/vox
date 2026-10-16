package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseTurnJpaEntity;

public final class AssessmentItemResponseTurnMapper {

    private AssessmentItemResponseTurnMapper() {
    }

    public static AssessmentItemResponseTurn toDomain(AssessmentItemResponseTurnJpaEntity jpa) {
        return new AssessmentItemResponseTurn(
            jpa.getId(),
            jpa.getAssessmentItemResponseId(),
            jpa.getTurnOrder(),
            TurnType.valueOf(jpa.getTurnType()),
            jpa.getPromptText(),
            jpa.getAudioUrl(),
            jpa.getTranscript(),
            jpa.getDurationSeconds(),
            jpa.getWordCount(),
            jpa.getAnsweredAt(),
            jpa.getCreatedAt()
        );
    }

    public static AssessmentItemResponseTurnJpaEntity toJpa(AssessmentItemResponseTurn domain) {
        return new AssessmentItemResponseTurnJpaEntity(
            domain.getId(),
            domain.getAssessmentItemResponseId(),
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
