package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;

public class QuestionDtoMapper {

    public static QuestionDto toQuestionDto(Question question) {
        return new QuestionDto(
            question.getId(),
            question.getQuestionBankId(),
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
            valueOf(question.getSharing()),
            question.getSourceQuestionId(),
            question.isLocked(),
            question.getStatus().name(),
            question.getConfidentiality().name(),
            question.getSecurePoolId(),
            valueOf(question.getCreatedAt()),
            valueOf(question.getUpdatedAt()),
            question.getCreatedBy(),
            question.getUpdatedBy()
        );
    }

    public static List<QuestionDto> toQuestionDtoList(List<Question> list) {
        return list.stream()
            .map(QuestionDtoMapper::toQuestionDto)
            .toList();
    }

    public static PageResult<QuestionDto> toDtoPage(PageResult<Question> page) {
        return new PageResult<>(
            toQuestionDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(Instant date) {
        return date == null ? null : date.toString();
    }

    private static String valueOf(QuestionSharing sharing) {
        return sharing == null ? null : sharing.name();
    }
}
