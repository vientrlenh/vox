package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentScheduleJpaEntity;

public final class AssessmentScheduleMapper {

    private AssessmentScheduleMapper() {}

    public static AssessmentSchedule toDomain(AssessmentScheduleJpaEntity jpa) {
        return AssessmentSchedule.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .schoolRoomId(jpa.getSchoolRoomId())
                .startDate(jpa.getStartDate())
                .endDate(jpa.getEndDate())
                .status(statusFromString(jpa.getStatus()))
                .movedToScheduleId(jpa.getMovedToScheduleId())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentScheduleJpaEntity toJpa(AssessmentSchedule domain) {
        return AssessmentScheduleJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .schoolRoomId(domain.getSchoolRoomId())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .status(domain.getStatus().name())
                .movedToScheduleId(domain.getMovedToScheduleId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private static AssessmentScheduleStatus statusFromString(String status) {
        return status == null ? null : AssessmentScheduleStatus.valueOf(status);
    }
}
