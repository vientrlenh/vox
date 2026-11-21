package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticeSession;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeSessionJpaEntity;

public final class PracticeSessionMapper {

    private PracticeSessionMapper() {
    }

    public static PracticeSession toDomain(
            PracticeSessionJpaEntity entity) {
        return PracticeSession.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .practicePaperId(entity.getPracticePaperId())
                .targetFrameworkBandId(entity.getTargetFrameworkBandId())
                .chosenPracticeTopicId(entity.getChosenPracticeTopicId())
                .targetSubAttributesJson(entity.getTargetSubAttributesJson())
                .origin(entity.getOrigin())
                .offeredTopicIdsJson(entity.getOfferedTopicIdsJson())
                .overallScore(entity.getOverallScore())
                .startedAt(entity.getStartedAt())
                .endedAt(entity.getEndedAt())
                .lastHeartbeatAt(entity.getLastHeartbeatAt())
                .gradedSeconds(entity.getGradedSeconds())
                .status(entity.getStatus())
                .abandonDiagnosis(entity.getAbandonDiagnosis())
                .helpRequestCount(entity.getHelpRequestCount())
                .longPauseCount(entity.getLongPauseCount())
                .build();
    }

    public static PracticeSessionJpaEntity toJpa(
            PracticeSession session) {
        PracticeSessionJpaEntity entity = PracticeSessionJpaEntity.builder()
                .id(session.getId())
                .studentId(session.getStudentId())
                .practicePaperId(session.getPracticePaperId())
                .targetFrameworkBandId(session.getTargetFrameworkBandId())
                .chosenPracticeTopicId(session.getChosenPracticeTopicId())
                .targetSubAttributesJson(session.getTargetSubAttributesJson())
                .origin(session.getOrigin())
                .offeredTopicIdsJson(session.getOfferedTopicIdsJson())
                .startedAt(session.getStartedAt())
                .lastHeartbeatAt(session.getLastHeartbeatAt())
                .gradedSeconds(session.getGradedSeconds())
                .status(session.getStatus())
                .helpRequestCount(session.getHelpRequestCount())
                .longPauseCount(session.getLongPauseCount())
                .build();
        entity.setOverallScore(session.getOverallScore());
        entity.setEndedAt(session.getEndedAt());
        entity.setAbandonDiagnosis(session.getAbandonDiagnosis());
        return entity;
    }
}
