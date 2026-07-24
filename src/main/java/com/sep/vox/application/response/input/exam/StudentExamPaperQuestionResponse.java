package com.sep.vox.application.response.input.exam;

import java.util.UUID;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;

public record StudentExamPaperQuestionResponse(
    UUID id,
    int orderIndex,
    UUID sectionId,
    String sectionTitle,
    String sectionInstruction,
    StudentQuestionResponse question,
    QuestionAssetDto asset,
    QuestionEvaluationGuideDto evaluationGuide
) {
}
