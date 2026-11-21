package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseJpaEntity;

public final class AssessmentItemResponseMapper {

    private AssessmentItemResponseMapper() {}

    public static AssessmentItemResponse toDomain(AssessmentItemResponseJpaEntity jpa) {
        return AssessmentItemResponse.builder()
                .id(jpa.getId())
                .sessionId(jpa.getSessionId())
                .paperItemId(jpa.getPaperItemId())
                .audioUrl(jpa.getAudioUrl())
                .durationSeconds(jpa.getDurationSeconds())
                .transcript(jpa.getTranscript())
                .terminationReason(jpa.getTerminationReason())
                .submittedAt(jpa.getSubmittedAt())
                .build();
    }

    public static AssessmentItemResponseJpaEntity toJpa(AssessmentItemResponse domain) {
        return AssessmentItemResponseJpaEntity.builder()
                .id(domain.getId())
                .sessionId(domain.getSessionId())
                .paperItemId(domain.getPaperItemId())
                .audioUrl(domain.getAudioUrl())
                .durationSeconds(domain.getDurationSeconds())
                .transcript(domain.getTranscript())
                .terminationReason(domain.getTerminationReason())
                .submittedAt(domain.getSubmittedAt())
                .build();
    }
}
