package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.question.QuestionTopic;

public class QuestionTopicDtoMapper {

    public static QuestionTopicDto toDto(QuestionTopic domain) {
        return new QuestionTopicDto(
            domain.getId(),
            domain.getQuestionBankId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.createdByUserId().orElse(null),
            domain.updatedByUserId().orElse(null)
        );
    }

    public static List<QuestionTopicDto> toDtoList(List<QuestionTopic> list) {
        return list.stream()
            .map(QuestionTopicDtoMapper::toDto)
            .toList();
    }

    public static PageResult<QuestionTopicDto> toDtoPage(PageResult<QuestionTopic> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
