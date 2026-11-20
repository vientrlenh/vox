package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamBlueprintSection;

public interface ExamBlueprintSectionRepository {
    ExamBlueprintSection save(ExamBlueprintSection section);
    Optional<ExamBlueprintSection> findById(UUID id);
    List<ExamBlueprintSection> findByBlueprintVersionId(UUID blueprintVersionId);
    List<ExamBlueprintSection> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds);
    void deleteById(UUID id);
}
