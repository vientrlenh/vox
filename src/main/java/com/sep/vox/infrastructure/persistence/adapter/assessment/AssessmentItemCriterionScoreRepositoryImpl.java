package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.repository.assessment.AssessmentItemCriterionScoreRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentItemCriterionScoreMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentItemCriterionScoreRepository;

@Repository
public class AssessmentItemCriterionScoreRepositoryImpl implements AssessmentItemCriterionScoreRepository {

    private final SpringDataAssessmentItemCriterionScoreRepository springDataAssessmentItemCriterionScoreRepository;

    public AssessmentItemCriterionScoreRepositoryImpl(
            SpringDataAssessmentItemCriterionScoreRepository springDataAssessmentItemCriterionScoreRepository) {
        this.springDataAssessmentItemCriterionScoreRepository = springDataAssessmentItemCriterionScoreRepository;
    }

    @Override
    public List<AssessmentItemCriterionScore> saveAll(List<AssessmentItemCriterionScore> scores) {
        return springDataAssessmentItemCriterionScoreRepository.saveAll(
            scores.stream().map(AssessmentItemCriterionScoreMapper::toJpa).toList()
        ).stream().map(AssessmentItemCriterionScoreMapper::toDomain).toList();
    }

    @Override
    public List<AssessmentItemCriterionScore> findByEvaluationId(UUID evaluationId) {
        return springDataAssessmentItemCriterionScoreRepository.findByEvaluationId(evaluationId)
            .stream()
            .map(AssessmentItemCriterionScoreMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByEvaluationIdIn(java.util.Collection<UUID> evaluationIds) {
        springDataAssessmentItemCriterionScoreRepository.deleteByEvaluationIdIn(evaluationIds);
    }
}
