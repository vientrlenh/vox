package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;

public interface SpringDataRubricRepository extends JpaRepository<RubricJpaEntity, UUID> {
    boolean existsByOwnerTypeAndSchoolIdAndLanguageId(RubricOwnerType ownerType, UUID schoolId, UUID languageId);
    boolean existsByOwnerTypeAndLanguageId(RubricOwnerType ownerType, UUID languageId);
}
