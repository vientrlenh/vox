package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.shared.PageResult;

public record QuestionEvaluationGuideDto(
    UUID id,
    UUID questionId,
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {

    public static QuestionEvaluationGuideDto toDto(QuestionEvaluationGuide questionEvaluationGuide) {
        return new QuestionEvaluationGuideDto(
            questionEvaluationGuide.getId(),
            questionEvaluationGuide.getQuestionId(),
            questionEvaluationGuide.getExpectedContent(),
            questionEvaluationGuide.getKeyPoints(),
            questionEvaluationGuide.getAcceptableResponses(),
            questionEvaluationGuide.getOffTopicExamples(),
            questionEvaluationGuide.getScoringHints(),
            questionEvaluationGuide.getCommonMistakes()
        );
    }

    public static List<QuestionEvaluationGuideDto> toDtoList(List<QuestionEvaluationGuide> list) {
        return list.stream()
            .map(QuestionEvaluationGuideDto::toDto)
            .toList();
    }

    public static PageResult<QuestionEvaluationGuideDto> toDtoPage(PageResult<QuestionEvaluationGuide> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
