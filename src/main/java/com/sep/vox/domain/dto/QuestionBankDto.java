package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;

public record QuestionBankDto(
    UUID id,
    UUID learningLanguageId,
    UUID schoolId,
    String code,
    String name,
    String description,
    QuestionBankOwnerType ownerType,
    QuestionBankStatus status,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {

    public static QuestionBankDto toDto(QuestionBank questionBank) {
        return new QuestionBankDto(
            questionBank.getId(),
            questionBank.getLearningLanguageId(),
            questionBank.getSchoolId(),
            Code.valueOf(questionBank.getCode()),
            questionBank.getName(),
            questionBank.getDescription(),
            questionBank.getOwnerType(),
            questionBank.getStatus(),
            questionBank.getCreatedAt(),
            questionBank.getUpdatedAt(),
            questionBank.getCreatedBy(),
            questionBank.getUpdatedBy()
        );
    }

    public static List<QuestionBankDto> toDtoList(List<QuestionBank> list) {
        return list.stream()
            .map(QuestionBankDto::toDto)
            .toList();
    }

    public static PageResult<QuestionBankDto> toDtoPage(PageResult<QuestionBank> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
