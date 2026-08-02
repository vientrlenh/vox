package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticeSession;
import com.sep.vox.infrastructure.persistence.entity.PracticeSessionJpaEntity;

public final class PracticeSessionMapper {

    private PracticeSessionMapper() {
    }

    public static PracticeSession toDomain(
            PracticeSessionJpaEntity entity) {
        return new PracticeSession(
            entity.getId(),
            entity.getStudentId(),
            entity.getPracticePaperId(),
            entity.getRubricVersionId(),
            entity.getTargetFrameworkBandId(),
            entity.getChosenPracticeTopicId(),
            entity.getTargetSubAttributesJson(),
            entity.getOrigin(),
            entity.getOfferedTopicIdsJson(),
            entity.getOverallScore(),
            entity.getStartedAt(),
            entity.getEndedAt(),
            entity.getLastHeartbeatAt(),
            entity.getGradedSeconds(),
            entity.getStatus(),
            entity.getAbandonDiagnosis(),
            entity.getHelpRequestCount(),
            entity.getLongPauseCount()
        );
    }

    public static PracticeSessionJpaEntity toJpa(
            PracticeSession session) {
        var entity = new PracticeSessionJpaEntity(
            session.getId(),
            session.getStudentId(),
            session.getPracticePaperId(),
            session.getRubricVersionId(),
            session.getTargetFrameworkBandId(),
            session.getChosenPracticeTopicId(),
            session.getTargetSubAttributesJson(),
            session.getOrigin(),
            session.getOfferedTopicIdsJson(),
            session.getStartedAt(),
            session.getLastHeartbeatAt(),
            session.getGradedSeconds(),
            session.getStatus(),
            session.getHelpRequestCount(),
            session.getLongPauseCount()
        );
        entity.setOverallScore(session.getOverallScore());
        entity.setEndedAt(session.getEndedAt());
        entity.setAbandonDiagnosis(session.getAbandonDiagnosis());
        return entity;
    }
}
