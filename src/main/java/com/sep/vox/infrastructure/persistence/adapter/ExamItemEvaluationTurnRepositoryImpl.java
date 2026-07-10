package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamItemEvaluationTurn;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamItemEvaluationTurnMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemEvaluationTurnRepository;

@Repository
public class ExamItemEvaluationTurnRepositoryImpl implements ExamItemEvaluationTurnRepository {

    private final SpringDataExamItemEvaluationTurnRepository springDataExamItemEvaluationTurnRepository;

    public ExamItemEvaluationTurnRepositoryImpl(
            SpringDataExamItemEvaluationTurnRepository springDataExamItemEvaluationTurnRepository) {
        this.springDataExamItemEvaluationTurnRepository = springDataExamItemEvaluationTurnRepository;
    }

    @Override
    public List<ExamItemEvaluationTurn> saveAll(List<ExamItemEvaluationTurn> turns) {
        return springDataExamItemEvaluationTurnRepository.saveAll(
            turns.stream().map(ExamItemEvaluationTurnMapper::toJpa).toList()
        ).stream().map(ExamItemEvaluationTurnMapper::toDomain).toList();
    }

    @Override
    public List<ExamItemEvaluationTurn> findByEvaluationId(UUID evaluationId) {
        return springDataExamItemEvaluationTurnRepository.findByEvaluationIdOrderByTurnOrderAsc(evaluationId)
            .stream()
            .map(ExamItemEvaluationTurnMapper::toDomain)
            .toList();
    }
}
