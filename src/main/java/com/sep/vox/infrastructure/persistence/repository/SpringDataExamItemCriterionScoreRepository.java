package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamItemCriterionScoreJpaEntity;

public interface SpringDataExamItemCriterionScoreRepository extends JpaRepository<ExamItemCriterionScoreJpaEntity, UUID> {
    List<ExamItemCriterionScoreJpaEntity> findByEvaluationId(UUID evaluationId);
}
