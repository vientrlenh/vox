package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultStatusHistoryJpaEntity;

public final class AssessmentResultStatusHistoryMapper {

    private AssessmentResultStatusHistoryMapper() {}

    public static AssessmentResultStatusHistory toDomain(AssessmentResultStatusHistoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AssessmentResultStatusHistory.builder()
                .id(entity.getId())
                .candidateResultId(entity.getCandidateResultId())
                .fromStatus(toResultStatus(entity.getFromStatus()))
                .toStatus(toResultStatus(entity.getToStatus()))
                .scoreBefore(entity.getScoreBefore())
                .scoreAfter(entity.getScoreAfter())
                .source(toSource(entity.getSource()))
                .actorId(entity.getActorId())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static AssessmentResultStatusHistoryJpaEntity toJpa(AssessmentResultStatusHistory history) {
        if (history == null) {
            return null;
        }
        return AssessmentResultStatusHistoryJpaEntity.builder()
                .id(history.getId())
                .candidateResultId(history.getCandidateResultId())
                .fromStatus(history.getFromStatus() == null ? null : history.getFromStatus().name())
                .toStatus(history.getToStatus() == null ? null : history.getToStatus().name())
                .scoreBefore(history.getScoreBefore())
                .scoreAfter(history.getScoreAfter())
                .source(history.getSource() == null ? null : history.getSource().name())
                .actorId(history.getActorId())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private static AssessmentCandidateResultStatus toResultStatus(String value) {
        return value == null ? null : AssessmentCandidateResultStatus.valueOf(value);
    }

    private static ResultStatusChangeSource toSource(String value) {
        return value == null ? null : ResultStatusChangeSource.valueOf(value);
    }
}
