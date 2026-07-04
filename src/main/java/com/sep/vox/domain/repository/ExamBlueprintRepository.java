package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamBlueprint;

public interface ExamBlueprintRepository {
    ExamBlueprint save(ExamBlueprint blueprint);
    Optional<ExamBlueprint> findById(UUID id);
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
    void deleteById(UUID id);
}
