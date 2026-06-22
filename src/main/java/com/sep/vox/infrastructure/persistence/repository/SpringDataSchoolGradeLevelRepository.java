package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeLevelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataSchoolGradeLevelRepository extends JpaRepository<SchoolGradeLevelJpaEntity, UUID> {
    Optional<SchoolGradeLevelJpaEntity> findBySchoolId(UUID schoolId);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
    boolean existsBySchoolIdAndOrder(UUID schoolId, int order);
}

