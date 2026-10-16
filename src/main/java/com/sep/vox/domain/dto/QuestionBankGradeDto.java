package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionBankGrade;

public record QuestionBankGradeDto(
    UUID id,
    UUID questionBankId,
    UUID schoolGradeId,
    Instant attachedAt,
    UUID attachedBy
) {

    public static QuestionBankGradeDto toDto(QuestionBankGrade questionBankGrade) {
        return new QuestionBankGradeDto(
            questionBankGrade.getId(),
            questionBankGrade.getQuestionBankId(),
            questionBankGrade.getSchoolGradeId(),
            questionBankGrade.getAttachedAt(),
            questionBankGrade.getAttachedBy()
        );
    }

    public static List<QuestionBankGradeDto> toDtoList(List<QuestionBankGrade> grades) {
        return grades.stream()
            .map(QuestionBankGradeDto::toDto)
            .toList();
    }
}
