package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentProctoringAlertJpaEntity;

public final class AssessmentProctoringAlertMapper {

    private AssessmentProctoringAlertMapper() {
    }

    public static AssessmentProctoringAlert toDomain(AssessmentProctoringAlertJpaEntity entity) {
        return new AssessmentProctoringAlert(
            entity.getId(),
            entity.getEventId(),
            entity.getAssessmentSessionId(),
            entity.getCandidateId(),
            entity.getStreamId(),
            entity.getStreamType(),
            entity.getAlertType(),
            entity.getLevel(),
            entity.getSource(),
            entity.getDetail(),
            entity.getConfidence(),
            entity.getSequenceNo(),
            entity.getCapturedAt(),
            entity.getRaisedAt(),
            entity.getCreatedAt()
        );
    }

    public static AssessmentProctoringAlertJpaEntity toJpa(AssessmentProctoringAlert alert) {
        var entity = new AssessmentProctoringAlertJpaEntity();
        entity.setId(alert.getId());
        entity.setEventId(alert.getEventId());
        entity.setAssessmentSessionId(alert.getAssessmentSessionId());
        entity.setCandidateId(alert.getCandidateId());
        entity.setStreamId(alert.getStreamId());
        entity.setStreamType(alert.getStreamType());
        entity.setAlertType(alert.getAlertType());
        entity.setLevel(alert.getLevel());
        entity.setSource(alert.getSource());
        entity.setDetail(alert.getDetail());
        entity.setConfidence(alert.getConfidence());
        entity.setSequenceNo(alert.getSequenceNo());
        entity.setCapturedAt(alert.getCapturedAt());
        entity.setRaisedAt(alert.getRaisedAt());
        entity.setCreatedAt(alert.getCreatedAt());
        return entity;
    }
}
