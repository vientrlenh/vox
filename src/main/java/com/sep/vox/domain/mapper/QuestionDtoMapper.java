package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.question.Question;

public class QuestionDtoMapper {

    public static QuestionDto toDto(Question domain) {
        return new QuestionDto(
            domain.getId(),
            domain.getTopicId(),
            domain.getQuestionText(),
            domain.getAudioUrl(),
            domain.getDifficultyLevel().value(),
            domain.getQuestionType().value(),
            domain.getDurationSeconds(),
            domain.isActive(),
            domain.getCreatedAt() != null ? domain.getCreatedAt().toString() : null
        );
    }

    public static List<QuestionDto> toDtoList(List<Question> list) {
        return list.stream()
            .map(QuestionDtoMapper::toDto)
            .toList();
    }

    public static PageResult<QuestionDto> toDtoPage(PageResult<Question> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
