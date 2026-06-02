package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricApplicabilityJpaEntity;

public interface SpringDataRubricApplicabilityRepository extends JpaRepository<RubricApplicabilityJpaEntity, UUID> {
    boolean existsBySchoolClassId(UUID schoolClassId);
}
