package com.sep.vox.application.mapper.question;

import java.util.List;

import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.Question;

public final class CreateQuestionResponseMapper {

    private CreateQuestionResponseMapper() {
    }

    public static CreateQuestionResponse toResponse(
            Question question,
            List<QuestionAssetDto> assets,
            QuestionEvaluationGuideDto evaluationGuide) {
        return new CreateQuestionResponse(
            QuestionDtoMapper.toQuestionDto(question),
            assets,
            evaluationGuide
        );
    }
}
