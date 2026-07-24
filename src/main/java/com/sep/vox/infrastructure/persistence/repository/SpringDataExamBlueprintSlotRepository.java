package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintSlotJpaEntity;

public interface SpringDataExamBlueprintSlotRepository extends JpaRepository<ExamBlueprintSlotJpaEntity, UUID> {
    List<ExamBlueprintSlotJpaEntity> findByBlueprintVersionIdOrderByOrderAsc(UUID blueprintVersionId);
    List<ExamBlueprintSlotJpaEntity> findByBlueprintVersionIdInOrderByOrderAsc(Collection<UUID> blueprintVersionIds);
    List<ExamBlueprintSlotJpaEntity> findBySectionIdOrderByOrderAsc(UUID sectionId);
    List<ExamBlueprintSlotJpaEntity> findBySectionIdInOrderByOrderAsc(Collection<UUID> sectionIds);
}
