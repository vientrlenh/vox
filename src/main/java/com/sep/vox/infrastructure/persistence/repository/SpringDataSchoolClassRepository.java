package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SpringDataSchoolClassRepository extends JpaRepository<SchoolClassJpaEntity, UUID>{
    Page<SchoolClassJpaEntity> findBySchoolId(UUID schoolId, Pageable pageable);
    Optional<SchoolClassJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolClassJpaEntity> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolClassJpaEntity> findBySchoolIdAndName(UUID schoolId, String name);
    List<SchoolClassJpaEntity> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId, UUID levelVersionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClassJpaEntity c
        SET c.name = :name,
            c.description = :description,
            c.targetSchoolLevelVersionId = :targetSchoolLevelVersionId,
            c.status = :status,
            c.updatedAt = :updatedAt,
            c.updatedBy = :updatedBy
        WHERE c.id = :id
            AND c.schoolId = :schoolId
            AND c.languageId = :languageId
    """)
    int updateMutableFields(
            @Param("id") UUID id,
            @Param("schoolId") UUID schoolId,
            @Param("languageId") UUID languageId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("targetSchoolLevelVersionId") UUID targetSchoolLevelVersionId,
            @Param("status") String status,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy);
}
