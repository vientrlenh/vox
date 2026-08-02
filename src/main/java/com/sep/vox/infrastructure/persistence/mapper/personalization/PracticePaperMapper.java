package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.infrastructure.persistence.entity.PracticePaperJpaEntity;

public final class PracticePaperMapper {

    private PracticePaperMapper() {
    }

    public static PracticePaper toDomain(PracticePaperJpaEntity entity) {
        return new PracticePaper(
            entity.getId(),
            entity.getStudentId(),
            entity.getPracticeTopicId(),
            entity.getOrigin(),
            entity.getGoalType(),
            entity.getOfferedTopicIdsJson(),
            entity.getPreviousOfferedTopicIdsJson(),
            entity.getPlannedSeconds(),
            entity.getReservedQuotaSeconds(),
            entity.getReservationExpiresAt(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }

    public static PracticePaperJpaEntity toJpa(PracticePaper paper) {
        return new PracticePaperJpaEntity(
            paper.id(),
            paper.studentId(),
            paper.practiceTopicId(),
            paper.origin(),
            paper.goalAtBuild(),
            paper.offeredTopicIdsJson(),
            paper.previousOfferedTopicIdsJson(),
            paper.plannedSeconds(),
            paper.reservedQuotaSeconds(),
            paper.expiresAt(),
            paper.status(),
            paper.createdAt()
        );
    }
}
