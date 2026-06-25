package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.AssessmentPolicyJpaEntity;

public interface SpringDataAssessmentPolicyRepository extends JpaRepository<AssessmentPolicyJpaEntity, UUID> {

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
}
