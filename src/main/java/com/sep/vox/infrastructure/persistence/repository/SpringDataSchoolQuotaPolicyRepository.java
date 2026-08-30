package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolQuotaPolicyJpaEntity;

public interface SpringDataSchoolQuotaPolicyRepository extends JpaRepository<SchoolQuotaPolicyJpaEntity, UUID> {
    Optional<SchoolQuotaPolicyJpaEntity> findBySchoolIdAndQuotaType(UUID schoolId, String quotaType);
    List<SchoolQuotaPolicyJpaEntity> findBySchoolId(UUID schoolId);
}
