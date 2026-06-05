package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;

public class QuestionEvaluationGuideDtoMapper {

    public static QuestionEvaluationGuideDto toDto(QuestionEvaluationGuide domain) {
        return new QuestionEvaluationGuideDto(
            domain.getId(),
            domain.getQuestionId(),
            domain.getExpectedContent(),
            domain.getKeyPoints(),
            domain.getAcceptableResponses(),
            domain.getOffTopicExamples(),
            domain.getScoringHints(),
            domain.getCommonMistakes()
        );
    }

    public static List<QuestionEvaluationGuideDto> toDtoList(List<QuestionEvaluationGuide> list) {
        return list.stream()
            .map(QuestionEvaluationGuideDtoMapper::toDto)
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
