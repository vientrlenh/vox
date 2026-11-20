package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamProctoringAlert;
import com.sep.vox.infrastructure.persistence.entity.ExamProctoringAlertJpaEntity;

public final class ExamProctoringAlertMapper {

    private ExamProctoringAlertMapper() {
    }

    public static ExamProctoringAlert toDomain(ExamProctoringAlertJpaEntity entity) {
        return new ExamProctoringAlert(
            entity.getId(),
            entity.getEventId(),
            entity.getExamSessionId(),
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

    public static ExamProctoringAlertJpaEntity toJpa(ExamProctoringAlert alert) {
        var entity = new ExamProctoringAlertJpaEntity();
        entity.setId(alert.getId());
        entity.setEventId(alert.getEventId());
        entity.setExamSessionId(alert.getExamSessionId());
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
