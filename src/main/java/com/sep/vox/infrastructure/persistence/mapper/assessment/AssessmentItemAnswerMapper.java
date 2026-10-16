package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswer;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemAnswerJpaEntity;

public final class AssessmentItemAnswerMapper {

    private AssessmentItemAnswerMapper() {}

    public static AssessmentItemAnswer toDomain(AssessmentItemAnswerJpaEntity jpa) {
        return new AssessmentItemAnswer(
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

    public static AssessmentItemAnswerJpaEntity toJpa(AssessmentItemAnswer domain) {
        return new AssessmentItemAnswerJpaEntity(
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
