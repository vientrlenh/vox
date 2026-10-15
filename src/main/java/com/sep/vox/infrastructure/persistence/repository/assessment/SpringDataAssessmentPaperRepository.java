package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperJpaEntity;

public interface SpringDataAssessmentPaperRepository extends JpaRepository<AssessmentPaperJpaEntity, UUID> {
    List<AssessmentPaperJpaEntity> findByExamIdOrderByVariantAsc(UUID examId);
    List<AssessmentPaperJpaEntity> findByExamIdInOrderByVariantAsc(java.util.Collection<UUID> examIds);
    List<AssessmentPaperJpaEntity> findByExamIdAndStatusOrderByVariantAsc(UUID examId, String status);
    boolean existsByExamId(UUID examId);
    void deleteByExamId(UUID examId);

    @Query("SELECT COALESCE(MAX(p.variant), 0) + 1 FROM AssessmentPaperJpaEntity p WHERE p.examId = :examId")
    int nextVariant(@Param("examId") UUID examId);
}
