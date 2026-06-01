package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricLevelMappingJpaEntity;

public interface SpringDataRubricLevelMappingRepository extends JpaRepository<RubricLevelMappingJpaEntity, UUID> {
}
