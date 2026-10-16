package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintSectionJpaEntity;

public interface SpringDataAssessmentBlueprintSectionRepository extends JpaRepository<AssessmentBlueprintSectionJpaEntity, UUID> {
    List<AssessmentBlueprintSectionJpaEntity> findByBlueprintVersionIdOrderByOrderAsc(UUID blueprintVersionId);
    List<AssessmentBlueprintSectionJpaEntity> findByBlueprintVersionIdInOrderByOrderAsc(Collection<UUID> blueprintVersionIds);
}
