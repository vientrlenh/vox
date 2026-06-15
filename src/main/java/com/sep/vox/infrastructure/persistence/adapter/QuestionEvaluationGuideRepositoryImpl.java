package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionEvaluationGuideMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionEvaluationGuideRepository;

@Repository
public class QuestionEvaluationGuideRepositoryImpl implements QuestionEvaluationGuideRepository {

    private final SpringDataQuestionEvaluationGuideRepository springDataQuestionEvaluationGuideRepository;

    public QuestionEvaluationGuideRepositoryImpl(SpringDataQuestionEvaluationGuideRepository springDataQuestionEvaluationGuideRepository) {
        this.springDataQuestionEvaluationGuideRepository = springDataQuestionEvaluationGuideRepository;
    }

    @Override
    public QuestionEvaluationGuide save(QuestionEvaluationGuide questionEvaluationGuide) {
        var entity = QuestionEvaluationGuideMapper.toJpa(questionEvaluationGuide);
        var saved = springDataQuestionEvaluationGuideRepository.save(entity);
        return QuestionEvaluationGuideMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionEvaluationGuide> findByQuestionId(UUID questionId) {
        return springDataQuestionEvaluationGuideRepository.findByQuestionId(questionId)
            .map(QuestionEvaluationGuideMapper::toDomain);
    }

    @Override
    public void deleteByQuestionId(UUID questionId) {
        springDataQuestionEvaluationGuideRepository.deleteByQuestionId(questionId);
    }

    @Override
    public void flush() {
        springDataQuestionEvaluationGuideRepository.flush();
    }
}
