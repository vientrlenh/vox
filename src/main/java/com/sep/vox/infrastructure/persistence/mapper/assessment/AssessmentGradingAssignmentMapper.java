package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingOutcome;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;

public final class AssessmentGradingAssignmentMapper {

    private AssessmentGradingAssignmentMapper() {}

    public static AssessmentGradingAssignment toDomain(AssessmentGradingAssignmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssessmentGradingAssignment(
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

    public static AssessmentGradingAssignmentJpaEntity toJpa(AssessmentGradingAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new AssessmentGradingAssignmentJpaEntity(
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

    private static AssessmentGradingAssignmentStatus toStatus(String value) {
        return value == null ? null : AssessmentGradingAssignmentStatus.valueOf(value);
    }

    private static AssessmentGradingRoundType toRoundType(String value) {
        return value == null ? null : AssessmentGradingRoundType.valueOf(value);
    }

    private static AssessmentGradingOutcome toOutcome(String value) {
        return value == null ? null : AssessmentGradingOutcome.valueOf(value);
    }
}
