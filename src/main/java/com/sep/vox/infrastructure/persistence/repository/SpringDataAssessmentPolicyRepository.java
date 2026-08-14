package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CurrentPolicyInfo;
import com.sep.vox.infrastructure.persistence.entity.AssessmentPolicyJpaEntity;

public interface SpringDataAssessmentPolicyRepository extends JpaRepository<AssessmentPolicyJpaEntity, UUID> {
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);

    boolean existsBySchoolClassId(UUID schoolClassId);

    @Query(value = """
        SELECT policy.rubric_version_id AS rubricVersionId,
               policy.target_framework_band_id AS targetFrameworkBandId
        FROM school_class_users membership
        JOIN school_classes class ON class.id = membership.school_class_id
        JOIN assessment_policies policy
          ON policy.status = 'PUBLISHED'
         AND policy.effective_from <= CURRENT_TIMESTAMP
         AND (policy.effective_to IS NULL OR policy.effective_to >= CURRENT_TIMESTAMP)
         AND (policy.school_id IS NULL OR policy.school_id = class.school_id)
         AND (policy.school_grade_id IS NULL OR policy.school_grade_id = class.school_grade_id)
         AND (policy.school_class_id IS NULL OR policy.school_class_id = class.id)
        WHERE membership.user_id = :studentId
          AND membership.is_active = true
        ORDER BY (policy.school_class_id IS NOT NULL) DESC,
                 (policy.school_grade_id IS NOT NULL) DESC,
                 (policy.school_id IS NOT NULL) DESC,
                 policy.version DESC
        LIMIT 1
        """, nativeQuery = true)
    List<CurrentPolicyInfo> findCurrentPolicyForStudent(@Param("studentId") UUID studentId);

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
            AND (p.effectiveFrom >= COALESCE(:effectiveFrom, p.effectiveFrom))
            AND (p.effectiveFrom <= COALESCE(:effectiveTo, p.effectiveFrom))
    """)
    Page<AssessmentPolicyJpaEntity> findBySchoolIdIsNullAndStatus(@Param("status") String status, @Param("languageId") UUID languageId,
            @Param("rubricVersionId") UUID rubricVersionId, @Param("effectiveFrom") Instant effectiveFrom,
            @Param("effectiveTo") Instant effectiveTo, Pageable pageable);

    @Query("""
        SELECT p FROM AssessmentPolicyJpaEntity p
        WHERE p.schoolId = :schoolId
            AND (:status IS NULL OR p.status = :status)
            AND (:languageId IS NULL OR p.languageId = :languageId)
            AND (:rubricVersionId IS NULL OR p.rubricVersionId = :rubricVersionId)
            AND (p.effectiveFrom >= COALESCE(:effectiveFrom, p.effectiveFrom))
            AND (p.effectiveFrom <= COALESCE(:effectiveTo, p.effectiveFrom))
    """)
    Page<AssessmentPolicyJpaEntity> findBySchoolIdAndStatus(@Param("schoolId") UUID schoolId, @Param("status") String status,
            @Param("languageId") UUID languageId, @Param("rubricVersionId") UUID rubricVersionId,
            @Param("effectiveFrom") Instant effectiveFrom, @Param("effectiveTo") Instant effectiveTo,
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
        @Param("atTime") Instant atTime, 
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

    /**
     * Giống {@code existsActiveForScope} nhưng KHÔNG lọc theo Rubric Version: một phạm vi chỉ được
     * có đúng một chính sách còn hiệu lực, bất kể nó trỏ vào phiên bản Rubric nào.
     */
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
            AND p.status IN ('DRAFT', 'PUBLISHED')
    """)
    boolean existsActiveForScopeAnyRubricVersion(
        @Param("schoolId") UUID schoolId,
        @Param("languageId") UUID languageId,
        @Param("frameworkVersionId") UUID frameworkVersionId,
        @Param("schoolGradeLevelId") UUID schoolGradeLevelId,
        @Param("schoolGradeId") UUID schoolGradeId,
        @Param("schoolClassId") UUID schoolClassId
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

    @Query("""
        SELECT p FROM AssessmentPolicyJpaEntity p
        WHERE (:schoolId IS NULL OR p.schoolId = :schoolId)
            AND (:schoolId IS NOT NULL OR p.schoolId IS NULL)
    """)
    List<AssessmentPolicyJpaEntity> findAllForOwner(@Param("schoolId") UUID schoolId);
}
