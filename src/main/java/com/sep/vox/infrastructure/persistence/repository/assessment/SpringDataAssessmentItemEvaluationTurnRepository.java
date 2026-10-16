package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemEvaluationTurnJpaEntity;

public interface SpringDataAssessmentItemEvaluationTurnRepository extends JpaRepository<AssessmentItemEvaluationTurnJpaEntity, UUID> {
    List<AssessmentItemEvaluationTurnJpaEntity> findByEvaluationIdOrderByTurnOrderAsc(UUID evaluationId);
    void deleteByEvaluationIdIn(java.util.Collection<UUID> evaluationIds);
}
