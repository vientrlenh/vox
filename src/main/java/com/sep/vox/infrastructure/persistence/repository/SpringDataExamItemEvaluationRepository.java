package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationJpaEntity;

public interface SpringDataExamItemEvaluationRepository extends JpaRepository<ExamItemEvaluationJpaEntity, UUID> {
    Optional<ExamItemEvaluationJpaEntity> findTopByResponseIdOrderByEvaluatedAtDesc(UUID responseId);
}
