package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentProctoringAlertJpaEntity;

public final class AssessmentProctoringAlertMapper {

    private AssessmentProctoringAlertMapper() {
    }

    public static AssessmentProctoringAlert toDomain(AssessmentProctoringAlertJpaEntity entity) {
        return AssessmentProctoringAlert.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .assessmentSessionId(entity.getAssessmentSessionId())
                .candidateId(entity.getCandidateId())
                .streamId(entity.getStreamId())
                .streamType(entity.getStreamType())
                .alertType(entity.getAlertType())
                .level(entity.getLevel())
                .source(entity.getSource())
                .detail(entity.getDetail())
                .confidence(entity.getConfidence())
                .sequenceNo(entity.getSequenceNo())
                .capturedAt(entity.getCapturedAt())
                .raisedAt(entity.getRaisedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static AssessmentProctoringAlertJpaEntity toJpa(AssessmentProctoringAlert alert) {
        return AssessmentProctoringAlertJpaEntity.builder()
                .id(alert.getId())
                .eventId(alert.getEventId())
                .assessmentSessionId(alert.getAssessmentSessionId())
                .candidateId(alert.getCandidateId())
                .streamId(alert.getStreamId())
                .streamType(alert.getStreamType())
                .alertType(alert.getAlertType())
                .level(alert.getLevel())
                .source(alert.getSource())
                .detail(alert.getDetail())
                .confidence(alert.getConfidence())
                .sequenceNo(alert.getSequenceNo())
                .capturedAt(alert.getCapturedAt())
                .raisedAt(alert.getRaisedAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
