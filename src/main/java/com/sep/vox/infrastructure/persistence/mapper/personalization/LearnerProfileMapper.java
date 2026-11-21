package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.LearnerProfile;
import com.sep.vox.infrastructure.persistence.entity.personalization.LearnerProfileJpaEntity;

public final class LearnerProfileMapper {

    private LearnerProfileMapper() {
    }

    public static LearnerProfile toDomain(LearnerProfileJpaEntity entity) {
        return LearnerProfile.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .goalType(entity.getGoalType())
                .autoUpdateInterest(entity.isAutoUpdateInterest())
                .quizCompletedAt(entity.getQuizCompletedAt())
                .recordedAt(entity.getRecordedAt())
                .build();
    }

    /**
     * MUST carry the {@code id}: the profile is now updated in place, and JPA tells insert from update
     * by the primary key itself. Without the id every save inserts a new row -- and the unique index
     * on {@code student_id} throws from the second save onwards.
     */
    public static LearnerProfileJpaEntity toJpa(LearnerProfile profile) {
        LearnerProfileJpaEntity entity = LearnerProfileJpaEntity.builder()
                .studentId(profile.getStudentId())
                .goalType(profile.getGoalType())
                .autoUpdateInterest(profile.isAutoUpdateInterest())
                .quizCompletedAt(profile.getQuizCompletedAt())
                .recordedAt(profile.getRecordedAt())
                .build();
        entity.setId(profile.getId());
        return entity;
    }
}
