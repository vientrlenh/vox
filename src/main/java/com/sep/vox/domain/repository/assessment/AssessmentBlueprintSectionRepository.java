package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;

public interface AssessmentBlueprintSectionRepository {
    AssessmentBlueprintSection save(AssessmentBlueprintSection section);
    Optional<AssessmentBlueprintSection> findById(UUID id);
    List<AssessmentBlueprintSection> findByBlueprintVersionId(UUID blueprintVersionId);
    List<AssessmentBlueprintSection> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds);
    void deleteById(UUID id);
}
