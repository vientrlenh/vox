package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamBlueprintSlot;

public interface ExamBlueprintSlotRepository {
    ExamBlueprintSlot save(ExamBlueprintSlot slot);
    Optional<ExamBlueprintSlot> findById(UUID id);
    List<ExamBlueprintSlot> findByIdIn(Collection<UUID> ids);
    List<ExamBlueprintSlot> findByBlueprintVersionId(UUID blueprintVersionId);
    List<ExamBlueprintSlot> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds);
    List<ExamBlueprintSlot> findBySectionId(UUID sectionId);
    List<ExamBlueprintSlot> findBySectionIdIn(Collection<UUID> sectionIds);
    void deleteById(UUID id);
}
