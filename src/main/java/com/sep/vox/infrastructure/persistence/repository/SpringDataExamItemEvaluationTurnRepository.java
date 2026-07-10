package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationTurnJpaEntity;

public interface SpringDataExamItemEvaluationTurnRepository extends JpaRepository<ExamItemEvaluationTurnJpaEntity, UUID> {
    List<ExamItemEvaluationTurnJpaEntity> findByEvaluationIdOrderByTurnOrderAsc(UUID evaluationId);
}
