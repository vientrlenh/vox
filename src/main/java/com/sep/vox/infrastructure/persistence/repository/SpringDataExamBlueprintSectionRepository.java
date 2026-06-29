package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintSectionJpaEntity;

public interface SpringDataExamBlueprintSectionRepository extends JpaRepository<ExamBlueprintSectionJpaEntity, UUID> {
    List<ExamBlueprintSectionJpaEntity> findByBlueprintVersionIdOrderByOrderAsc(UUID blueprintVersionId);
}
