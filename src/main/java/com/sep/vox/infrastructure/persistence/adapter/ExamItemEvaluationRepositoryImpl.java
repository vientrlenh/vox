package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamItemEvaluationMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemEvaluationRepository;

@Repository
public class ExamItemEvaluationRepositoryImpl implements ExamItemEvaluationRepository {

    private final SpringDataExamItemEvaluationRepository springDataExamItemEvaluationRepository;

    public ExamItemEvaluationRepositoryImpl(
            SpringDataExamItemEvaluationRepository springDataExamItemEvaluationRepository) {
        this.springDataExamItemEvaluationRepository = springDataExamItemEvaluationRepository;
    }

    @Override
    public ExamItemEvaluation save(ExamItemEvaluation evaluation) {
        var saved = springDataExamItemEvaluationRepository.save(ExamItemEvaluationMapper.toJpa(evaluation));
        return ExamItemEvaluationMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamItemEvaluation> findLatestByResponseId(UUID responseId) {
        return springDataExamItemEvaluationRepository.findTopByResponseIdOrderByEvaluatedAtDesc(responseId)
            .map(ExamItemEvaluationMapper::toDomain);
    }

    @Override
    public List<ExamItemEvaluation> findLatestByResponseIdIn(Collection<UUID> responseIds) {
        return springDataExamItemEvaluationRepository.findLatestByResponseIdIn(responseIds).stream()
            .map(ExamItemEvaluationMapper::toDomain)
            .toList();
    }
}
