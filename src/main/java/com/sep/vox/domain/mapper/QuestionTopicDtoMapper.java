package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.questiontopic.QuestionTopic;

public class QuestionTopicDtoMapper {

    public static QuestionTopicDto toDto(QuestionTopic domain) {
        return new QuestionTopicDto(
            domain.getId(),
            domain.getBankId(),
            domain.getTopicName(),
            domain.getDescription()
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
