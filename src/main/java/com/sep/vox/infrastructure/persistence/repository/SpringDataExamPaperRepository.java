package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamPaperJpaEntity;

public interface SpringDataExamPaperRepository extends JpaRepository<ExamPaperJpaEntity, UUID> {
    List<ExamPaperJpaEntity> findByExamIdOrderByVariantAsc(UUID examId);
    List<ExamPaperJpaEntity> findByExamIdInOrderByVariantAsc(java.util.Collection<UUID> examIds);
    List<ExamPaperJpaEntity> findByExamIdAndStatusOrderByVariantAsc(UUID examId, String status);
    boolean existsByExamId(UUID examId);
    void deleteByExamId(UUID examId);

    @Query("SELECT COALESCE(MAX(p.variant), 0) + 1 FROM ExamPaperJpaEntity p WHERE p.examId = :examId")
    int nextVariant(@Param("examId") UUID examId);
}
