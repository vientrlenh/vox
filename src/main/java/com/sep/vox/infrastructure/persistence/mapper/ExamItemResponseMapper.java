package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseJpaEntity;

public final class ExamItemResponseMapper {

    private ExamItemResponseMapper() {}

    public static ExamItemResponse toDomain(ExamItemResponseJpaEntity jpa) {
        return new ExamItemResponse(
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

    public static ExamItemResponseJpaEntity toJpa(ExamItemResponse domain) {
        return new ExamItemResponseJpaEntity(
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
