package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamScheduleJpaEntity;

public final class ExamScheduleMapper {

    private ExamScheduleMapper() {}

    public static ExamSchedule toDomain(ExamScheduleJpaEntity jpa) {
        return new ExamSchedule(
            jpa.getId(),
            jpa.getExamId(),
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

    public static ExamScheduleJpaEntity toJpa(ExamSchedule domain) {
        return new ExamScheduleJpaEntity(
            domain.getId(),
            domain.getExamId(),
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

    private static ExamScheduleStatus statusFromString(String status) {
        return status == null ? null : ExamScheduleStatus.valueOf(status);
    }
}
