package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.shared.PageResult;

public record QuestionDto(
    UUID id,
    UUID questionBankId,
    UUID questionTopicId,
    String code,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    QuestionType type,
    int preparationTimeSeconds,
    int minResponseSeconds,
    int maxResponseSeconds,
    QuestionSharing sharing,
    UUID sourceQuestionId,
    boolean locked,
    QuestionStatus status,
    QuestionConfidentiality confidentiality,
    UUID securePoolId,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {

    public static QuestionDto toDto(Question question) {
        return new QuestionDto(
            question.getId(),
            question.getQuestionBankId(),
            question.getQuestionTopicId(),
            question.getCode(),
            question.getInstructionText(),
            question.getQuestionText(),
            question.getPromptText(),
            question.getPreparationText(),
            question.getType(),
            question.getPreparationTimeSeconds(),
            question.getMinResponseSeconds(),
            question.getMaxResponseSeconds(),
            question.getSharing(),
            question.getSourceQuestionId(),
            question.isLocked(),
            question.getStatus(),
            question.getConfidentiality(),
            question.getSecurePoolId(),
            question.getCreatedAt(),
            question.getUpdatedAt(),
            question.getCreatedBy(),
            question.getUpdatedBy()
        );
    }

    public static List<QuestionDto> toDtoList(List<Question> list) {
        return list.stream()
            .map(QuestionDto::toDto)
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
