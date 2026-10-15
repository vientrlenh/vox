package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.shared.PageResult;

public interface AssessmentBlueprintRepository {
    AssessmentBlueprint save(AssessmentBlueprint blueprint);
    Optional<AssessmentBlueprint> findById(UUID id);
    List<AssessmentBlueprint> findByIdIn(Collection<UUID> ids);
    PageResult<AssessmentBlueprint> findAccessible(
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
