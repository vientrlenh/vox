package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseJpaEntity;

public final class AssessmentItemResponseMapper {

    private AssessmentItemResponseMapper() {}

    public static AssessmentItemResponse toDomain(AssessmentItemResponseJpaEntity jpa) {
        return new AssessmentItemResponse(
            jpa.getId(),
            jpa.getSessionId(),
            jpa.getPaperItemId(),
            jpa.getAudioUrl(),
            jpa.getDurationSeconds(),
            jpa.getTranscript(),
            jpa.getTerminationReason(),
            jpa.getSubmittedAt()
        );
    }

    public static AssessmentItemResponseJpaEntity toJpa(AssessmentItemResponse domain) {
        return new AssessmentItemResponseJpaEntity(
            domain.getId(),
            domain.getSessionId(),
            domain.getPaperItemId(),
            domain.getAudioUrl(),
            domain.getDurationSeconds(),
            domain.getTranscript(),
            domain.getTerminationReason(),
            domain.getSubmittedAt()
        );
    }
}
