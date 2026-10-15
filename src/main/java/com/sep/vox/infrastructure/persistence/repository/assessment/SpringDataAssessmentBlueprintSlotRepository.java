package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintSlotJpaEntity;

public interface SpringDataAssessmentBlueprintSlotRepository extends JpaRepository<AssessmentBlueprintSlotJpaEntity, UUID> {
    List<AssessmentBlueprintSlotJpaEntity> findByBlueprintVersionIdOrderByOrderAsc(UUID blueprintVersionId);
    List<AssessmentBlueprintSlotJpaEntity> findByBlueprintVersionIdInOrderByOrderAsc(Collection<UUID> blueprintVersionIds);
    List<AssessmentBlueprintSlotJpaEntity> findBySectionIdOrderByOrderAsc(UUID sectionId);
    List<AssessmentBlueprintSlotJpaEntity> findBySectionIdInOrderByOrderAsc(Collection<UUID> sectionIds);
}
