package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperJpaEntity;

public interface SpringDataAssessmentPaperRepository extends JpaRepository<AssessmentPaperJpaEntity, UUID> {
    List<AssessmentPaperJpaEntity> findByAssessmentIdOrderByVariantAsc(UUID assessmentId);
    List<AssessmentPaperJpaEntity> findByAssessmentIdInOrderByVariantAsc(java.util.Collection<UUID> assessmentIds);
    List<AssessmentPaperJpaEntity> findByAssessmentIdAndStatusOrderByVariantAsc(UUID assessmentId, String status);
    boolean existsByAssessmentId(UUID assessmentId);
    void deleteByAssessmentId(UUID assessmentId);

    @Query("SELECT COALESCE(MAX(p.variant), 0) + 1 FROM AssessmentPaperJpaEntity p WHERE p.assessmentId = :assessmentId")
    int nextVariant(@Param("assessmentId") UUID assessmentId);
}
