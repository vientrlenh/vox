package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.shared.PageResult;

public record QuestionTopicDto(
    UUID id,
    UUID questionBankId,
    String code,
    String name,
    String description,
    QuestionTopicStatus status,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {

    public static QuestionTopicDto toDto(QuestionTopic questionTopic) {
        return new QuestionTopicDto(
            questionTopic.getId(),
            questionTopic.getQuestionBankId(),
            questionTopic.getCode(),
            questionTopic.getName(),
            questionTopic.getDescription(),
            questionTopic.getStatus(),
            questionTopic.getCreatedAt(),
            questionTopic.getUpdatedAt(),
            questionTopic.getCreatedBy(),
            questionTopic.getUpdatedBy()
        );
    }

    public static List<QuestionTopicDto> toDtoList(List<QuestionTopic> list) {
        return list.stream()
            .map(QuestionTopicDto::toDto)
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
