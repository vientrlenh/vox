package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.question.Question;

public class QuestionDtoMapper {

    public static QuestionDto toDto(Question domain) {
        return new QuestionDto(
            domain.getId(),
            domain.getQuestionTopicId(),
            domain.getCode(),
            domain.getInstructionText(),
            domain.getQuestionText(),
            domain.getPromptText(),
            domain.getPreparationText(),
            domain.getType().name(),
            domain.getPreparationTimeSeconds(),
            domain.getMinResponseSeconds(),
            domain.getMaxResponseSeconds(),
            domain.getScope().name(),
            domain.getVisibility().name(),
            domain.getSourceQuestionId(),
            domain.isLocked(),
            domain.getStatus().name(),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
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

    private static String valueOf(OffsetDateTime date) {
        return date == null ? null : date.toString();
    }
}
