package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;

public final class AssessmentGradingAssignmentMapper {

    private AssessmentGradingAssignmentMapper() {}

    public static AssessmentGradingAssignment toDomain(AssessmentGradingAssignmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AssessmentGradingAssignment.builder()
                .id(entity.getId())
                .candidateResultId(entity.getCandidateResultId())
                .teacherId(entity.getTeacherId())
                .roundType(toRoundType(entity.getRoundType()))
                .appealId(entity.getAppealId())
                .status(toStatus(entity.getStatus()))
                .outcome(toOutcome(entity.getOutcome()))
                .scoreBefore(entity.getScoreBefore())
                .assignedAt(entity.getAssignedAt())
                .assignedBy(entity.getAssignedBy())
                .completedAt(entity.getCompletedAt())
                .deadlineAt(entity.getDeadlineAt())
                .remindedAt(entity.getRemindedAt())
                .reason(entity.getReason())
                .activeResultId(entity.getActiveResultId())
                .build();
    }

    public static AssessmentGradingAssignmentJpaEntity toJpa(AssessmentGradingAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return AssessmentGradingAssignmentJpaEntity.builder()
                .id(assignment.getId())
                .candidateResultId(assignment.getCandidateResultId())
                .teacherId(assignment.getTeacherId())
                .roundType(assignment.getRoundType() == null ? null : assignment.getRoundType().name())
                .appealId(assignment.getAppealId())
                .status(assignment.getStatus() == null ? null : assignment.getStatus().name())
                .outcome(assignment.getOutcome() == null ? null : assignment.getOutcome().name())
                .scoreBefore(assignment.getScoreBefore())
                .assignedAt(assignment.getAssignedAt())
                .assignedBy(assignment.getAssignedBy())
                .completedAt(assignment.getCompletedAt())
                .deadlineAt(assignment.getDeadlineAt())
                .remindedAt(assignment.getRemindedAt())
                .reason(assignment.getReason())
                .activeResultId(assignment.getActiveResultId())
                .build();
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
