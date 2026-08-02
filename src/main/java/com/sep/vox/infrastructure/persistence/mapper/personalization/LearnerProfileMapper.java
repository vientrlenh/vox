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
            entity.getVersion(),
            entity.getGoalType(),
            entity.getTargetExam(),
            entity.getTargetDate(),
            entity.getFlsaScore(),
            entity.getFlsaRawAnswersJson(),
            entity.isAutoUpdateInterest(),
            entity.getQuizCompletedAt(),
            entity.getRecordedAt()
        );
    }

    public static LearnerProfileJpaEntity toJpa(LearnerProfile profile) {
        return new LearnerProfileJpaEntity(
            profile.studentId(),
            profile.version(),
            profile.goalType(),
            profile.targetExam(),
            profile.targetDate(),
            profile.flsaScore(),
            profile.flsaRawAnswersJson(),
            profile.autoUpdateInterest(),
            profile.quizCompletedAt(),
            profile.recordedAt()
        );
    }
}
