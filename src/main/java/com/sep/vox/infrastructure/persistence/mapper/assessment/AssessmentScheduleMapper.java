package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentScheduleJpaEntity;

public final class AssessmentScheduleMapper {

    private AssessmentScheduleMapper() {}

    public static AssessmentSchedule toDomain(AssessmentScheduleJpaEntity jpa) {
        return new AssessmentSchedule(
            jpa.getId(),
            jpa.getAssessmentId(),
            jpa.getSchoolRoomId(),
            jpa.getStartDate(),
            jpa.getEndDate(),
            statusFromString(jpa.getStatus()),
            jpa.getMovedToScheduleId(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentScheduleJpaEntity toJpa(AssessmentSchedule domain) {
        return new AssessmentScheduleJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getSchoolRoomId(),
            domain.getStartDate(),
            domain.getEndDate(),
            domain.getStatus().name(),
            domain.getMovedToScheduleId(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static AssessmentScheduleStatus statusFromString(String status) {
        return status == null ? null : AssessmentScheduleStatus.valueOf(status);
    }
}
