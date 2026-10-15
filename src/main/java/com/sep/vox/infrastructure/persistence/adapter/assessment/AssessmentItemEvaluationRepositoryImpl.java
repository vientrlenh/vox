package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.repository.assessment.AssessmentItemEvaluationRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentItemEvaluationMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentItemEvaluationRepository;

@Repository
public class AssessmentItemEvaluationRepositoryImpl implements AssessmentItemEvaluationRepository {

    private final SpringDataAssessmentItemEvaluationRepository springDataAssessmentItemEvaluationRepository;

    public AssessmentItemEvaluationRepositoryImpl(
            SpringDataAssessmentItemEvaluationRepository springDataAssessmentItemEvaluationRepository) {
        this.springDataAssessmentItemEvaluationRepository = springDataAssessmentItemEvaluationRepository;
    }

    @Override
    public AssessmentItemEvaluation save(AssessmentItemEvaluation evaluation) {
        var saved = springDataAssessmentItemEvaluationRepository.save(AssessmentItemEvaluationMapper.toJpa(evaluation));
        return AssessmentItemEvaluationMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentItemEvaluation> findLatestByResponseId(UUID responseId) {
        return springDataAssessmentItemEvaluationRepository.findLatestByResponseId(responseId)
            .map(AssessmentItemEvaluationMapper::toDomain);
    }

    @Override
    public Optional<AssessmentItemEvaluation> findLatestAiByResponseId(UUID responseId) {
        return springDataAssessmentItemEvaluationRepository.findLatestAiByResponseId(responseId)
            .map(AssessmentItemEvaluationMapper::toDomain);
    }

    @Override
    public List<AssessmentItemEvaluation> findLatestByResponseIdIn(Collection<UUID> responseIds) {
        return springDataAssessmentItemEvaluationRepository.findLatestByResponseIdIn(responseIds).stream()
            .map(AssessmentItemEvaluationMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentItemEvaluation> findByResponseIdIn(Collection<UUID> responseIds) {
        return springDataAssessmentItemEvaluationRepository.findByResponseIdIn(responseIds).stream()
            .map(AssessmentItemEvaluationMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByResponseIdIn(Collection<UUID> responseIds) {
        springDataAssessmentItemEvaluationRepository.deleteByResponseIdIn(responseIds);
    }
}
