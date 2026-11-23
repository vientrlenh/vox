package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;
import com.sep.vox.domain.repository.AssessmentItemEvaluationTurnRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentItemEvaluationTurnMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentItemEvaluationTurnRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentItemEvaluationTurnRepositoryImpl implements AssessmentItemEvaluationTurnRepository {

    private final SpringDataAssessmentItemEvaluationTurnRepository springDataAssessmentItemEvaluationTurnRepository;

    @Override
    public List<AssessmentItemEvaluationTurn> saveAll(List<AssessmentItemEvaluationTurn> turns) {
        return springDataAssessmentItemEvaluationTurnRepository.saveAll(
            turns.stream().map(AssessmentItemEvaluationTurnMapper::toJpa).toList()
        ).stream().map(AssessmentItemEvaluationTurnMapper::toDomain).toList();
    }

    @Override
    public List<AssessmentItemEvaluationTurn> findByEvaluationId(UUID evaluationId) {
        return springDataAssessmentItemEvaluationTurnRepository.findByEvaluationIdOrderByTurnOrderAsc(evaluationId)
            .stream()
            .map(AssessmentItemEvaluationTurnMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByEvaluationIdIn(Collection<UUID> evaluationIds) {
        springDataAssessmentItemEvaluationTurnRepository.deleteByEvaluationIdIn(evaluationIds);
    }
}
