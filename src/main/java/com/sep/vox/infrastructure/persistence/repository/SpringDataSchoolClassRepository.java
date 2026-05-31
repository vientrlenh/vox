package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SpringDataSchoolClassRepository extends JpaRepository<SchoolClassJpaEntity, UUID>{
    Optional<SchoolClassJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolClassJpaEntity> findBySchoolIdAndName(UUID schoolId, String name);
    List<SchoolClassJpaEntity> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId, UUID levelVersionId);
}
