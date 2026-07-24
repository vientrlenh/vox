package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
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
            toStatus(entity.getStatus()),
            entity.getAssignedAt(),
            entity.getAssignedBy(),
            entity.getCompletedAt()
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
            assignment.getStatus() == null ? null : assignment.getStatus().name(),
            assignment.getAssignedAt(),
            assignment.getAssignedBy(),
            assignment.getCompletedAt()
        );
    }

    private static GradingAssignmentStatus toStatus(String value) {
        return value == null ? null : GradingAssignmentStatus.valueOf(value);
    }
}
