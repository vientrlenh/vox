package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.LearnerProfile;
import com.sep.vox.infrastructure.persistence.entity.LearnerProfileJpaEntity;

public final class LearnerProfileMapper {

    private LearnerProfileMapper() {
    }

    public static LearnerProfile toDomain(LearnerProfileJpaEntity entity) {
        return new LearnerProfile(
            entity.getId(),
            entity.getStudentId(),
            entity.getGoalType(),
            entity.isAutoUpdateInterest(),
            entity.getQuizCompletedAt(),
            entity.getRecordedAt()
        );
    }

    /**
     * MUST carry the {@code id}: the profile is now updated in place, and JPA tells insert from update
     * by the primary key itself. Without the id every save inserts a new row -- and the unique index
     * on {@code student_id} throws from the second save onwards.
     */
    public static LearnerProfileJpaEntity toJpa(LearnerProfile profile) {
        LearnerProfileJpaEntity entity = new LearnerProfileJpaEntity(
            profile.getStudentId(),
            profile.getGoalType(),
            profile.isAutoUpdateInterest(),
            profile.getQuizCompletedAt(),
            profile.getRecordedAt()
        );
        entity.setId(profile.getId());
        return entity;
    }
}
