package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentScheduleProctorJpaEntity;

public final class AssessmentScheduleProctorMapper {

    private AssessmentScheduleProctorMapper() {}

    public static AssessmentScheduleProctor toDomain(AssessmentScheduleProctorJpaEntity jpa) {
        return AssessmentScheduleProctor.builder()
                .id(jpa.getId())
                .scheduleId(jpa.getScheduleId())
                .teacherId(jpa.getTeacherId())
                .build();
    }

    public static AssessmentScheduleProctorJpaEntity toJpa(AssessmentScheduleProctor domain) {
        return AssessmentScheduleProctorJpaEntity.builder()
                .id(domain.getId())
                .scheduleId(domain.getScheduleId())
                .teacherId(domain.getTeacherId())
                .build();
    }
}
