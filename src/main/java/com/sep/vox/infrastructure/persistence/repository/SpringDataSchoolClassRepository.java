package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SpringDataSchoolClassRepository extends JpaRepository<SchoolClassJpaEntity, UUID>{
    Page<SchoolClassJpaEntity> findBySchoolId(UUID schoolId, Pageable pageable);
    Optional<SchoolClassJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolClassJpaEntity> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolClassJpaEntity> findBySchoolIdAndName(UUID schoolId, String name);
    List<SchoolClassJpaEntity> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId, UUID levelVersionId);
}
