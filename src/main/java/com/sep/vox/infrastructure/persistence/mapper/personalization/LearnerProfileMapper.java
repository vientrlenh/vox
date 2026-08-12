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
     * PHẢI mang theo {@code id}: hồ sơ nay cập nhật tại chỗ, mà JPA phân biệt insert với update
     * bằng chính khoá chính. Thiếu id thì mỗi lần lưu lại chèn một dòng mới -- và unique index
     * trên {@code student_id} sẽ ném ngay từ lần thứ hai.
     */
    public static LearnerProfileJpaEntity toJpa(LearnerProfile profile) {
        var entity = new LearnerProfileJpaEntity(
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
