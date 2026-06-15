package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;

public interface QuestionEvaluationGuideRepository {
    QuestionEvaluationGuide save(QuestionEvaluationGuide questionEvaluationGuide);
    Optional<QuestionEvaluationGuide> findByQuestionId(UUID questionId);
    void deleteByQuestionId(UUID questionId);
    void flush();
}
