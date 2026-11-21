package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseTurnJpaEntity;

public final class AssessmentItemResponseTurnMapper {

    private AssessmentItemResponseTurnMapper() {
    }

    public static AssessmentItemResponseTurn toDomain(AssessmentItemResponseTurnJpaEntity jpa) {
        return AssessmentItemResponseTurn.builder()
                .id(jpa.getId())
                .assessmentItemResponseId(jpa.getAssessmentItemResponseId())
                .turnOrder(jpa.getTurnOrder())
                .turnType(TurnType.valueOf(jpa.getTurnType()))
                .promptText(jpa.getPromptText())
                .audioUrl(jpa.getAudioUrl())
                .transcript(jpa.getTranscript())
                .durationSeconds(jpa.getDurationSeconds())
                .wordCount(jpa.getWordCount())
                .answeredAt(jpa.getAnsweredAt())
                .createdAt(jpa.getCreatedAt())
                .build();
    }

    public static AssessmentItemResponseTurnJpaEntity toJpa(AssessmentItemResponseTurn domain) {
        return AssessmentItemResponseTurnJpaEntity.builder()
                .id(domain.getId())
                .assessmentItemResponseId(domain.getAssessmentItemResponseId())
                .turnOrder(domain.getTurnOrder())
                .turnType(domain.getTurnType().name())
                .promptText(domain.getPromptText())
                .audioUrl(domain.getAudioUrl())
                .transcript(domain.getTranscript())
                .durationSeconds(domain.getDurationSeconds())
                .wordCount(domain.getWordCount())
                .answeredAt(domain.getAnsweredAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
