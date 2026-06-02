package com.sep.vox.infrastructure.persistence.adapter;

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
}
