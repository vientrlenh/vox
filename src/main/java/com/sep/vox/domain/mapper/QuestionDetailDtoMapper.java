package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionDetailDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.question.Question;

public class QuestionDetailDtoMapper {

    public static QuestionDetailDto toDto(
            Question question,
            QuestionTopicDto questionTopic,
            QuestionEvaluationGuideDto evaluationGuide,
            List<QuestionAssetDto> assets) {
        return new QuestionDetailDto(
            question.getId(),
            question.getQuestionTopicId(),
            question.getCode(),
            question.getInstructionText(),
            question.getQuestionText(),
            question.getPromptText(),
            question.getPreparationText(),
            question.getType().name(),
            question.getPreparationTimeSeconds(),
            question.getMinResponseSeconds(),
            question.getMaxResponseSeconds(),
            question.getScope().name(),
            question.getVisibility().name(),
            question.getSourceQuestionId(),
            question.isLocked(),
            question.getStatus().name(),
            valueOf(question.getCreatedAt()),
            valueOf(question.getUpdatedAt()),
            questionTopic,
            evaluationGuide,
            assets
        );
    }

    private static String valueOf(OffsetDateTime date) {
        return date == null ? null : date.toString();
    }
}
