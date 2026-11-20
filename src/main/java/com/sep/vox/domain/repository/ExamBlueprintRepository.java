package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamBlueprint;

public interface ExamBlueprintRepository {
    ExamBlueprint save(ExamBlueprint blueprint);
    Optional<ExamBlueprint> findById(UUID id);
    List<ExamBlueprint> findByIdIn(Collection<UUID> ids);
    PageResult<ExamBlueprint> findAccessible(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID schoolId,
        Boolean isActive,
        UUID languageId,
        String examKind,
        String keyword,
        int page,
        int size
    );
    boolean existsUsedByExam(UUID blueprintId);
    boolean canEditBlueprint(UUID blueprintId, UUID userId, UUID schoolId);
    boolean canChangeVersionStatus(UUID blueprintId, UUID userId, UUID schoolId);

    /** CHAIR của một kỳ thi đang gắn blueprint này -- chỉ CHAIR, không tính REVIEWER. */
    boolean isChairOfExamUsingBlueprint(UUID blueprintId, UUID userId, UUID schoolId);
    boolean canViewBlueprint(UUID blueprintId, UUID userId, UUID schoolId);
    void deleteById(UUID id);
}
