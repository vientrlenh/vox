package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolDebtEventJpaEntity;

public interface SpringDataSchoolDebtEventRepository extends JpaRepository<SchoolDebtEventJpaEntity, UUID> {
    List<SchoolDebtEventJpaEntity> findBySchoolId(UUID schoolId);
}
