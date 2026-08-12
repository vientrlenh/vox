package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionEvaluationInfo;

public interface PracticeQuestionQueryRepository {

    /** Câu hỏi kèm tên/mô tả chủ đề của nó -- đủ để dựng payload chấm, không cần aggregate. */
    Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId);
}
