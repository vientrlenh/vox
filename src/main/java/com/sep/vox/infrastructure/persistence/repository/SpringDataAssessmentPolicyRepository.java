package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.AssessmentPolicyJpaEntity;

public interface SpringDataAssessmentPolicyRepository extends JpaRepository<AssessmentPolicyJpaEntity, UUID> {
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);

    boolean existsByRubricVersionIdAndStatus(UUID rubricVersionId, String status);

    boolean existsByRubricVersionIdAndStatusNot(UUID rubricVersionId, String status);

    List<AssessmentPolicyJpaEntity> findBySchoolIdIsNullAndRubricVersionIdAndStatus(UUID rubricVersionId, String status);

    List<AssessmentPolicyJpaEntity> findBySchoolIdAndRubricVersionIdAndStatus(UUID schoolId, UUID rubricVersionId, String status);

    @Query("""
        SELECT p FROM AssessmentPolicyJpaEntity p
        WHERE p.schoolId IS NULL
            AND (:status IS NULL OR p.status = :status)
            AND (:languageId IS NULL OR p.languageId = :languageId)
            AND (:rubricVersionId IS NULL OR p.rubricVersionId = :rubricVersionId)
            AND (p.effectiveTo IS NULL OR p.effectiveTo >= COALESCE(:effectiveFrom, p.effectiveTo))
            AND (p.effectiveFrom <= COALESCE(:effectiveTo, p.effectiveFrom))
    """)
    Page<AssessmentPolicyJpaEntity> findBySchoolIdIsNullAndStatus(@Param("status") String status, @Param("languageId") UUID languageId,
            @Param("rubricVersionId") UUID rubricVersionId, @Param("effectiveFrom") OffsetDateTime effectiveFrom,
            @Param("effectiveTo") OffsetDateTime effectiveTo, Pageable pageable);

    @Query("""
        SELECT p FROM AssessmentPolicyJpaEntity p
        WHERE p.schoolId = :schoolId
            AND (:status IS NULL OR p.status = :status)
            AND (:languageId IS NULL OR p.languageId = :languageId)
            AND (:rubricVersionId IS NULL OR p.rubricVersionId = :rubricVersionId)
            AND (p.effectiveTo IS NULL OR p.effectiveTo >= COALESCE(:effectiveFrom, p.effectiveTo))
            AND (p.effectiveFrom <= COALESCE(:effectiveTo, p.effectiveFrom))
    """)
    Page<AssessmentPolicyJpaEntity> findBySchoolIdAndStatus(@Param("schoolId") UUID schoolId, @Param("status") String status,
            @Param("languageId") UUID languageId, @Param("rubricVersionId") UUID rubricVersionId,
            @Param("effectiveFrom") OffsetDateTime effectiveFrom, @Param("effectiveTo") OffsetDateTime effectiveTo,
            Pageable pageable);


    @Query("""
        SELECT p FROM AssessmentPolicyJpaEntity p 
        WHERE p.schoolId = :schoolId
            AND p.languageId = :languageId 
            AND p.status = 'PUBLISHED' 
            AND p.effectiveFrom <= :atTime 
            AND (p.effectiveTo IS NULL OR p.effectiveTo >= :atTime) 
            AND (
                p.schoolClassId = :classId 
                OR (p.schoolClassId IS NULL AND p.schoolGradeId = :gradeId) 
                OR (p.schoolClassId IS NULL AND p.schoolGradeId IS NULL AND p.schoolGradeLevelId = :gradeLevelId)
            )    
        ORDER BY 
            CASE 
                WHEN p.schoolClassId = :classId THEN 0
                WHEN p.schoolGradeId = :gradeId THEN 1
                ELSE 2
            END, 
            p.version DESC
    """)
    List<AssessmentPolicyJpaEntity> findCandidatePolicies(
        @Param("schoolId") UUID schoolId, 
        @Param("languageId") UUID languageId, 
        @Param("classId") UUID classId, 
        @Param("gradeId") UUID gradeId, 
        @Param("gradeLevelId") UUID gradeLevelId, 
        @Param("atTime") OffsetDateTime atTime, 
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(p) > 0 FROM AssessmentPolicyJpaEntity p
        WHERE (:schoolId IS NULL OR p.schoolId = :schoolId)
            AND (:schoolId IS NOT NULL OR p.schoolId IS NULL)
            AND p.languageId = :languageId
            AND p.frameworkVersionId = :frameworkVersionId
            AND (:schoolGradeLevelId IS NULL OR p.schoolGradeLevelId = :schoolGradeLevelId)
            AND (:schoolGradeLevelId IS NOT NULL OR p.schoolGradeLevelId IS NULL)
            AND (:schoolGradeId IS NULL OR p.schoolGradeId = :schoolGradeId)
            AND (:schoolGradeId IS NOT NULL OR p.schoolGradeId IS NULL)
            AND (:schoolClassId IS NULL OR p.schoolClassId = :schoolClassId)
            AND (:schoolClassId IS NOT NULL OR p.schoolClassId IS NULL)
            AND p.rubricVersionId = :rubricVersionId
            AND p.status IN ('DRAFT', 'PUBLISHED')
    """)
    boolean existsActiveForScope(
        @Param("schoolId") UUID schoolId,
        @Param("languageId") UUID languageId,
        @Param("frameworkVersionId") UUID frameworkVersionId,
        @Param("schoolGradeLevelId") UUID schoolGradeLevelId,
        @Param("schoolGradeId") UUID schoolGradeId,
        @Param("schoolClassId") UUID schoolClassId,
        @Param("rubricVersionId") UUID rubricVersionId
    );

    @Query("""
        SELECT COALESCE(MAX(p.version), 0) FROM AssessmentPolicyJpaEntity p
        WHERE (:schoolId IS NULL OR p.schoolId = :schoolId)
            AND (:schoolId IS NOT NULL OR p.schoolId IS NULL)
            AND p.languageId = :languageId
            AND p.frameworkVersionId = :frameworkVersionId
            AND (:schoolGradeLevelId IS NULL OR p.schoolGradeLevelId = :schoolGradeLevelId)
            AND (:schoolGradeLevelId IS NOT NULL OR p.schoolGradeLevelId IS NULL)
            AND (:schoolGradeId IS NULL OR p.schoolGradeId = :schoolGradeId)
            AND (:schoolGradeId IS NOT NULL OR p.schoolGradeId IS NULL)
            AND (:schoolClassId IS NULL OR p.schoolClassId = :schoolClassId)
            AND (:schoolClassId IS NOT NULL OR p.schoolClassId IS NULL)
    """)
    int findMaxVersionForScope(
        @Param("schoolId") UUID schoolId,
        @Param("languageId") UUID languageId,
        @Param("frameworkVersionId") UUID frameworkVersionId,
        @Param("schoolGradeLevelId") UUID schoolGradeLevelId,
        @Param("schoolGradeId") UUID schoolGradeId,
        @Param("schoolClassId") UUID schoolClassId
    );
}
