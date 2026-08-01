package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.infrastructure.persistence.entity.ExamResultStatusHistoryJpaEntity;

public final class ExamResultStatusHistoryMapper {

    private ExamResultStatusHistoryMapper() {}

    public static ExamResultStatusHistory toDomain(ExamResultStatusHistoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExamResultStatusHistory(
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

    public static ExamResultStatusHistoryJpaEntity toJpa(ExamResultStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new ExamResultStatusHistoryJpaEntity(
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

    private static ExamCandidateResultStatus toResultStatus(String value) {
        return value == null ? null : ExamCandidateResultStatus.valueOf(value);
    }

    private static ResultStatusChangeSource toSource(String value) {
        return value == null ? null : ResultStatusChangeSource.valueOf(value);
    }
}
