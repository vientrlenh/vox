package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultStatusHistoryJpaEntity;

public final class AssessmentResultStatusHistoryMapper {

    private AssessmentResultStatusHistoryMapper() {}

    public static AssessmentResultStatusHistory toDomain(AssessmentResultStatusHistoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssessmentResultStatusHistory(
            entity.getId(),
            entity.getCandidateResultId(),
            toResultStatus(entity.getFromStatus()),
            toResultStatus(entity.getToStatus()),
            entity.getScoreBefore(),
            entity.getScoreAfter(),
            toSource(entity.getSource()),
            entity.getActorId(),
            entity.getReason(),
            entity.getCreatedAt()
        );
    }

    public static AssessmentResultStatusHistoryJpaEntity toJpa(AssessmentResultStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new AssessmentResultStatusHistoryJpaEntity(
            history.getId(),
            history.getCandidateResultId(),
            history.getFromStatus() == null ? null : history.getFromStatus().name(),
            history.getToStatus() == null ? null : history.getToStatus().name(),
            history.getScoreBefore(),
            history.getScoreAfter(),
            history.getSource() == null ? null : history.getSource().name(),
            history.getActorId(),
            history.getReason(),
            history.getCreatedAt()
        );
    }

    private static AssessmentCandidateResultStatus toResultStatus(String value) {
        return value == null ? null : AssessmentCandidateResultStatus.valueOf(value);
    }

    private static AssessmentResultStatusChangeSource toSource(String value) {
        return value == null ? null : AssessmentResultStatusChangeSource.valueOf(value);
    }
}
