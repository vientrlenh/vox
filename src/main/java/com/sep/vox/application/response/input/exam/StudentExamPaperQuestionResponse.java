package com.sep.vox.application.response.input.exam;

import java.util.UUID;

import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;

public record StudentExamPaperQuestionResponse(
    UUID id,
    int orderIndex,
    StudentQuestionResponse question,
    QuestionEvaluationGuideDto evaluationGuide
) {
}
