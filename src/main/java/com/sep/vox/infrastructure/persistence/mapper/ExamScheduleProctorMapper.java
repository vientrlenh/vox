package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.infrastructure.persistence.entity.ExamScheduleProctorJpaEntity;

public final class ExamScheduleProctorMapper {

    private ExamScheduleProctorMapper() {}

    public static ExamScheduleProctor toDomain(ExamScheduleProctorJpaEntity jpa) {
        return new ExamScheduleProctor(
            jpa.getId(),
            jpa.getScheduleId(),
            jpa.getTeacherId()
        );
    }

    public static ExamScheduleProctorJpaEntity toJpa(ExamScheduleProctor domain) {
        return new ExamScheduleProctorJpaEntity(
            domain.getId(),
            domain.getScheduleId(),
            domain.getTeacherId()
        );
    }
}
