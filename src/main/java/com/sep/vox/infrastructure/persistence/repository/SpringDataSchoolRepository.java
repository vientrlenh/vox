package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;

public interface SpringDataSchoolRepository extends JpaRepository<SchoolJpaEntity, UUID> {
    Optional<SchoolJpaEntity> findByCode(String code);
    Optional<SchoolJpaEntity> findByDomain(String domain);
    boolean existsByDomain(String domain);
    boolean existsByIdAndIsActiveTrue(UUID schoolId);
}
