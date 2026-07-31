package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;

public final class ExamGradingAssignmentMapper {

    private ExamGradingAssignmentMapper() {}

    public static ExamGradingAssignment toDomain(ExamGradingAssignmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExamGradingAssignment(
            entity.getId(),
            entity.getCandidateResultId(),
            entity.getTeacherId(),
            toRoundType(entity.getRoundType()),
            entity.getAppealId(),
            toStatus(entity.getStatus()),
            toOutcome(entity.getOutcome()),
            entity.getScoreBefore(),
            entity.getAssignedAt(),
            entity.getAssignedBy(),
            entity.getCompletedAt(),
            entity.getDeadlineAt(),
            entity.getRemindedAt(),
            entity.getReason(),
            entity.getActiveResultId()
        );
    }

    public static ExamGradingAssignmentJpaEntity toJpa(ExamGradingAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new ExamGradingAssignmentJpaEntity(
            assignment.getId(),
            assignment.getCandidateResultId(),
            assignment.getTeacherId(),
            assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
            assignment.getAppealId(),
            assignment.getStatus() == null ? null : assignment.getStatus().name(),
            assignment.getOutcome() == null ? null : assignment.getOutcome().name(),
            assignment.getScoreBefore(),
            assignment.getAssignedAt(),
            assignment.getAssignedBy(),
            assignment.getCompletedAt(),
            assignment.getDeadlineAt(),
            assignment.getRemindedAt(),
            assignment.getReason(),
            assignment.getActiveResultId()
        );
    }

    private static GradingAssignmentStatus toStatus(String value) {
        return value == null ? null : GradingAssignmentStatus.valueOf(value);
    }

    private static GradingRoundType toRoundType(String value) {
        return value == null ? null : GradingRoundType.valueOf(value);
    }

    private static GradingOutcome toOutcome(String value) {
        return value == null ? null : GradingOutcome.valueOf(value);
    }
}
