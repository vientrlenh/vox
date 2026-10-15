package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;

public interface AssessmentBlueprintSlotRepository {
    AssessmentBlueprintSlot save(AssessmentBlueprintSlot slot);
    Optional<AssessmentBlueprintSlot> findById(UUID id);
    List<AssessmentBlueprintSlot> findByIdIn(Collection<UUID> ids);
    List<AssessmentBlueprintSlot> findByBlueprintVersionId(UUID blueprintVersionId);
    List<AssessmentBlueprintSlot> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds);
    List<AssessmentBlueprintSlot> findBySectionId(UUID sectionId);
    List<AssessmentBlueprintSlot> findBySectionIdIn(Collection<UUID> sectionIds);
    void deleteById(UUID id);
}
