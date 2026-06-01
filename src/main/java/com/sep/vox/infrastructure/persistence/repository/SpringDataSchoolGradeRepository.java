package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;

public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID>{
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);
}
