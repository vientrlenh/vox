package com.sep.vox.infrastructure.persistence.repository;

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
    Optional<SchoolClassJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolClassJpaEntity> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);

    @Query("""
        SELECT sc
        FROM SchoolClassJpaEntity sc
        JOIN SchoolClassUserJpaEntity scu ON scu.schoolClassId = sc.id
        WHERE sc.schoolId = :schoolId
            AND scu.userId = :userId
        """)
    Page<SchoolClassJpaEntity> findByUserId(
        @Param("schoolId") UUID schoolId,
        @Param("userId") UUID userId,
        Pageable pageable);

    @Query("""
        SELECT sc
        FROM SchoolClassJpaEntity sc
        WHERE sc.schoolId = :schoolId
            AND (:status IS NULL OR sc.status = :status)
            AND (:languageId IS NULL OR sc.languageId = :languageId)
            AND (:schoolGradeId IS NULL OR sc.schoolGradeId = :schoolGradeId)
        """)
    Page<SchoolClassJpaEntity> findBySchoolIdWithFilters(
        @Param("schoolId") UUID schoolId,
        @Param("status") String status,
        @Param("languageId") UUID languageId,
        @Param("schoolGradeId") UUID schoolGradeId,
        Pageable pageable
    );

    @Query("""
        SELECT sc
        FROM SchoolClassJpaEntity sc
        WHERE sc.schoolId = :schoolId
            AND (LOWER(sc.code) LIKE :searchPattern
                OR LOWER(sc.name) LIKE :searchPattern)
            AND (:status IS NULL OR sc.status = :status)
            AND (:languageId IS NULL OR sc.languageId = :languageId)
            AND (:schoolGradeId IS NULL OR sc.schoolGradeId = :schoolGradeId)
        """)
    Page<SchoolClassJpaEntity> findBySchoolIdWithSearchAndFilters(
        @Param("schoolId") UUID schoolId,
        @Param("searchPattern") String searchPattern,
        @Param("status") String status,
        @Param("languageId") UUID languageId,
        @Param("schoolGradeId") UUID schoolGradeId,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClassJpaEntity sc
        SET sc.name = CASE WHEN :nameProvided = true THEN :name ELSE sc.name END,
            sc.description = CASE WHEN :descriptionProvided = true THEN :description ELSE sc.description END,
            sc.status = CASE WHEN :statusProvided = true THEN :status ELSE sc.status END,
            sc.updatedAt = :updatedAt,
            sc.updatedBy = :updatedBy
        WHERE sc.id = :id
            AND sc.schoolId = :schoolId
        """)
    int updateMutableFields(
        @Param("id") UUID id,
        @Param("schoolId") UUID schoolId,
        @Param("name") String name,
        @Param("nameProvided") boolean nameProvided,
        @Param("description") String description,
        @Param("descriptionProvided") boolean descriptionProvided,
        @Param("status") String status,
        @Param("statusProvided") boolean statusProvided,
        @Param("updatedAt") java.time.OffsetDateTime updatedAt,
        @Param("updatedBy") UUID updatedBy
    );

    @Query(value = """
        SELECT 
            id, 
            school_id, 
            language_id, 
            school_grade_id, 
            code, 
            name, 
            description, 
            status, 
            created_at, 
            updated_at, 
            created_by, 
            updated_by 
        FROM (
            SELECT 
                sc.*,
                row_number() OVER (
                    PARTITION BY sc.school_id 
                    ORDER BY sc.id DESC
                ) AS rn 
            FROM school_classes sc 
            WHERE sc.school_id IN (:schoolIds)
        ) ranked
        WHERE ranked.rn BETWEEN :fromRow AND :toRow
        ORDER BY ranked.school_id, ranked.rn
    """, nativeQuery = true)
    List<SchoolClassJpaEntity> findBySchoolIdIn(@Param("schoolIds") Collection<UUID> schoolIds, @Param("fromRow") int fromRow, @Param("toRow") int toRow);

    boolean existsBySchoolGradeId(UUID schoolGradeId);

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);
}
