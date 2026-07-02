package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
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
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
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

    private static String valueOf(OffsetDateTime date) {
        return date == null ? null : date.toString();
    }
}
