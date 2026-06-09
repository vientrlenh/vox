package com.sep.vox.infrastructure.persistence.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

public final class QuestionReadDtoMapper {

    public static QuestionDto toDto(QuestionJpaEntity e) {
        return new QuestionDto(
                e.getId(),
                e.getQuestionTopicId(),
                e.getCode(),
                e.getInstructionText(),
                e.getQuestionText(),
                e.getPromptText(),
                e.getPreparationText(),
                e.getType(),
                e.getPreparationTimeSeconds(),
                e.getMinResponseSeconds(),
                e.getMaxResponseSeconds(),
                e.getScope(),
                e.getVisibility(),
                e.getSourceQuestionId(),
                e.isLocked(),
                e.getStatus(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
        );
    }

    public static QuestionTopicDto toTopicDto(QuestionTopicJpaEntity e) {
        return new QuestionTopicDto(
                e.getId(),
                e.getQuestionBankId(),
                e.getCode(),
                e.getName(),
                e.getDescription(),
                e.getStatus(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
        );
    }

    public static PageResult<QuestionDto> toDtoPage(List<QuestionJpaEntity> entities, Long total, PageRequest page) {
        List<QuestionDto> dtos = entities.stream().map(QuestionReadDtoMapper::toDto).toList();
        int totalPages = (int) Math.ceil((double) total / page.size());
        return new PageResult<>(dtos, page.page(), page.size(), total, totalPages);
    }

    public static PageResult<QuestionTopicDto> toTopicDtoPage(List<QuestionTopicJpaEntity> entities, Long total, PageRequest page) {
        List<QuestionTopicDto> dtos = entities.stream().map(QuestionReadDtoMapper::toTopicDto).toList();
        int totalPages = (int) Math.ceil((double) total / page.size());
        return new PageResult<>(dtos, page.page(), page.size(), total, totalPages);
    }
}
