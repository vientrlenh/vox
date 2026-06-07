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

    @Query("""
        SELECT schoolClass
        FROM SchoolClassJpaEntity schoolClass
        WHERE schoolClass.schoolId = :schoolId
            AND (:search IS NULL
                OR LOWER(schoolClass.code) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(schoolClass.name) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR schoolClass.status = :status)
            AND (:languageId IS NULL OR schoolClass.languageId = :languageId)
            AND (:schoolGradeId IS NULL OR schoolClass.schoolGradeId = :schoolGradeId)
        """)
    Page<SchoolClassJpaEntity> findBySchoolIdWithFilters(
        @Param("schoolId") UUID schoolId,
        @Param("search") String search,
        @Param("status") String status,
        @Param("languageId") UUID languageId,
        @Param("schoolGradeId") UUID schoolGradeId,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClassJpaEntity schoolClass
        SET schoolClass.name = CASE WHEN :nameProvided = true THEN :name ELSE schoolClass.name END,
            schoolClass.description = CASE WHEN :descriptionProvided = true THEN :description ELSE schoolClass.description END,
            schoolClass.status = CASE WHEN :statusProvided = true THEN :status ELSE schoolClass.status END,
            schoolClass.updatedAt = :updatedAt,
            schoolClass.updatedBy = :updatedBy
        WHERE schoolClass.id = :id
            AND schoolClass.schoolId = :schoolId
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
            code, name, 
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
}
