package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentScheduleProctorJpaEntity;

public final class AssessmentScheduleProctorMapper {

    private AssessmentScheduleProctorMapper() {}

    public static AssessmentScheduleProctor toDomain(AssessmentScheduleProctorJpaEntity jpa) {
        return new AssessmentScheduleProctor(
            jpa.getId(),
            jpa.getScheduleId(),
            jpa.getTeacherId()
        );
    }

    public static AssessmentScheduleProctorJpaEntity toJpa(AssessmentScheduleProctor domain) {
        return new AssessmentScheduleProctorJpaEntity(
            domain.getId(),
            domain.getScheduleId(),
            domain.getTeacherId()
        );
    }
}
