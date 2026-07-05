package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamBlueprintSlot;

public interface ExamBlueprintSlotRepository {
    ExamBlueprintSlot save(ExamBlueprintSlot slot);
    Optional<ExamBlueprintSlot> findById(UUID id);
    List<ExamBlueprintSlot> findByBlueprintVersionId(UUID blueprintVersionId);
    List<ExamBlueprintSlot> findBySectionId(UUID sectionId);
    void deleteById(UUID id);
}
