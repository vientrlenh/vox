package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemCriterionScoreJpaEntity;

public interface SpringDataAssessmentItemCriterionScoreRepository extends JpaRepository<AssessmentItemCriterionScoreJpaEntity, UUID> {
    List<AssessmentItemCriterionScoreJpaEntity> findByEvaluationId(UUID evaluationId);
    void deleteByEvaluationIdIn(java.util.Collection<UUID> evaluationIds);
}
