package com.sep.vox.application.response.input.question;

import java.util.List;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.model.question.Question;

public record CreateQuestionResponse(
    QuestionDto question,
    List<QuestionAssetDto> assets,
    QuestionEvaluationGuideDto evaluationGuide
) {

    public static CreateQuestionResponse toResponse(
            Question question,
            List<QuestionAssetDto> assets,
            QuestionEvaluationGuideDto evaluationGuide) {
        return new CreateQuestionResponse(
            QuestionDto.toDto(question),
            assets,
            evaluationGuide
        );
    }
}
