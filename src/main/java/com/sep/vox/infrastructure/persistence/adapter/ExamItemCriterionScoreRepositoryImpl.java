package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamItemCriterionScoreMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemCriterionScoreRepository;

@Repository
public class ExamItemCriterionScoreRepositoryImpl implements ExamItemCriterionScoreRepository {

    private final SpringDataExamItemCriterionScoreRepository springDataExamItemCriterionScoreRepository;

    public ExamItemCriterionScoreRepositoryImpl(
            SpringDataExamItemCriterionScoreRepository springDataExamItemCriterionScoreRepository) {
        this.springDataExamItemCriterionScoreRepository = springDataExamItemCriterionScoreRepository;
    }

    @Override
    public List<ExamItemCriterionScore> saveAll(List<ExamItemCriterionScore> scores) {
        return springDataExamItemCriterionScoreRepository.saveAll(
            scores.stream().map(ExamItemCriterionScoreMapper::toJpa).toList()
        ).stream().map(ExamItemCriterionScoreMapper::toDomain).toList();
    }

    @Override
    public List<ExamItemCriterionScore> findByEvaluationId(UUID evaluationId) {
        return springDataExamItemCriterionScoreRepository.findByEvaluationId(evaluationId)
            .stream()
            .map(ExamItemCriterionScoreMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByEvaluationIdIn(java.util.Collection<UUID> evaluationIds) {
        springDataExamItemCriterionScoreRepository.deleteByEvaluationIdIn(evaluationIds);
    }
}
