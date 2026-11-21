package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticePaperJpaEntity;

public final class PracticePaperMapper {

    private PracticePaperMapper() {
    }

    public static PracticePaper toDomain(PracticePaperJpaEntity entity) {
        return PracticePaper.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .practiceTopicId(entity.getPracticeTopicId())
                .targetFrameworkBandId(entity.getTargetFrameworkBandId())
                .origin(entity.getOrigin())
                .goalAtBuild(entity.getGoalType())
                .offeredTopicIdsJson(entity.getOfferedTopicIdsJson())
                .previousOfferedTopicIdsJson(entity.getPreviousOfferedTopicIdsJson())
                .plannedSeconds(entity.getPlannedSeconds())
                .reservedQuotaSeconds(entity.getReservedQuotaSeconds())
                .expiresAt(entity.getReservationExpiresAt())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static PracticePaperJpaEntity toJpa(PracticePaper paper) {
        return PracticePaperJpaEntity.builder()
                .id(paper.getId())
                .studentId(paper.getStudentId())
                .practiceTopicId(paper.getPracticeTopicId())
                .targetFrameworkBandId(paper.getTargetFrameworkBandId())
                .origin(paper.getOrigin())
                .goalType(paper.getGoalAtBuild())
                .offeredTopicIdsJson(paper.getOfferedTopicIdsJson())
                .previousOfferedTopicIdsJson(paper.getPreviousOfferedTopicIdsJson())
                .plannedSeconds(paper.getPlannedSeconds())
                .reservedQuotaSeconds(paper.getReservedQuotaSeconds())
                .reservationExpiresAt(paper.getExpiresAt())
                .status(paper.getStatus())
                .createdAt(paper.getCreatedAt())
                .build();
    }
}
