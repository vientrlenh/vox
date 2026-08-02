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
            session.id(),
            session.studentId(),
            session.practicePaperId(),
            session.rubricVersionId(),
            session.targetFrameworkBandId(),
            session.chosenPracticeTopicId(),
            session.targetSubAttributesJson(),
            session.origin(),
            session.offeredTopicIdsJson(),
            session.startedAt(),
            session.lastHeartbeatAt(),
            session.gradedSeconds(),
            session.status(),
            session.helpRequestCount(),
            session.longPauseCount()
        );
        entity.setOverallScore(session.overallScore());
        entity.setEndedAt(session.endedAt());
        entity.setAbandonDiagnosis(session.abandonDiagnosis());
        return entity;
    }
}
