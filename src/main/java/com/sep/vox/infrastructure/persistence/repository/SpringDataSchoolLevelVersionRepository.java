package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolLevelVersionJpaEntity;

public interface SpringDataSchoolLevelVersionRepository extends JpaRepository<SchoolLevelVersionJpaEntity, UUID>{
    Optional<SchoolLevelVersionJpaEntity> findBySchoolLevelIdAndVersion(UUID schoolLevelId, int version);
}
