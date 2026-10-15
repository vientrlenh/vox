package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;

public interface AssessmentBlueprintVersionRepository {
    AssessmentBlueprintVersion save(AssessmentBlueprintVersion version);
    Optional<AssessmentBlueprintVersion> findById(UUID id);
    List<AssessmentBlueprintVersion> findByIdIn(Collection<UUID> ids);
    List<AssessmentBlueprintVersion> findByBlueprintId(UUID blueprintId);
    List<AssessmentBlueprintVersion> findByBlueprintIdIn(Collection<UUID> blueprintIds);
    List<AssessmentBlueprintVersion> findByBlueprintIdAndStatus(UUID blueprintId, AssessmentBlueprintVersionStatus status);
    int nextVersionNumber(UUID blueprintId);
    boolean existsUsedByVersion(UUID versionId);
    void deleteById(UUID id);
}
