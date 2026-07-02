package com.sep.vox.application.response.input.question;

import java.util.List;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;

public record CreateQuestionResponse(
    QuestionDto question,
    List<QuestionAssetDto> assets,
    QuestionEvaluationGuideDto evaluationGuide
) {
}
