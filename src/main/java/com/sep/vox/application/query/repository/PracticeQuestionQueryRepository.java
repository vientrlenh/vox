package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionEvaluationInfo;

public interface PracticeQuestionQueryRepository {

    /** A question with the name/description of its topic -- enough to build the grading payload, no aggregate needed. */
    Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId);
}
