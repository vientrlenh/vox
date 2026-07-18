package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;

public interface QuestionEvaluationGuideRepository {
    QuestionEvaluationGuide save(QuestionEvaluationGuide questionEvaluationGuide);
    Optional<QuestionEvaluationGuide> findByQuestionId(UUID questionId);
    List<QuestionEvaluationGuide> findByQuestionIdIn(Collection<UUID> questionIds);
    void deleteByQuestionId(UUID questionId);
}
