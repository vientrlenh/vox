package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricRepository extends JpaRepository<RubricJpaEntity, UUID> {
    boolean existsByOwnerTypeAndSchoolIdAndLanguageId(String ownerType, UUID schoolId, UUID languageId);

    boolean existsByOwnerTypeAndLanguageId(String ownerType, UUID languageId);


    @Modifying
    @Query("""
            
        UPDATE RubricJpaEntity r SET 
            r.name = COALESCE(:name, r.name),
            r.description = COALESCE(:description, r.description)
        WHERE r.id = :id
            """)
    int updateRubricAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description
    );


    Page<RubricJpaEntity> findAllByOwnerType(String ownerType, Pageable pageable);

    Page<RubricJpaEntity> findAllByOwnerTypeAndSchoolId(String ownerType, UUID schoolId, Pageable pageable);


    // 1. Search cho SYSTEM (Lọc cứng owner_type = 'SYSTEM')
    @Query("SELECT r FROM RubricJpaEntity r WHERE r.ownerType = 'SYSTEM' " +
            "AND (CAST(:keyword AS string) IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
            "AND (:frameworkId IS NULL OR r.frameworkId = :frameworkId) " +
            "AND (:languageId IS NULL OR r.languageId = :languageId)")
    Page<RubricJpaEntity> searchSystemRubrics(
            @Param("keyword") String keyword,
            @Param("frameworkId") UUID frameworkId,
            @Param("languageId") UUID languageId,
            Pageable pageable);

    // 2. Search cho SCHOOL (Lọc cứng theo school_id)
    @Query("SELECT r FROM RubricJpaEntity r WHERE r.schoolId = :schoolId " +
            "AND (CAST(:keyword AS string) IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
            "AND (:frameworkId IS NULL OR r.frameworkId = :frameworkId) " +
            "AND (:languageId IS NULL OR r.languageId = :languageId)")
    Page<RubricJpaEntity> searchSchoolRubrics(
            @Param("schoolId") UUID schoolId,
            @Param("keyword") String keyword,
            @Param("frameworkId") UUID frameworkId,
            @Param("languageId") UUID languageId,
            Pageable pageable);
}