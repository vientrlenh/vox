package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.QuestionBankGradeDto;
import com.sep.vox.domain.model.question.QuestionBankGrade;

public final class QuestionBankGradeDtoMapper {

    private QuestionBankGradeDtoMapper() {
    }

    public static QuestionBankGradeDto toDto(QuestionBankGrade grade) {
        return new QuestionBankGradeDto(
            grade.getId(),
            grade.getQuestionBankId(),
            grade.getSchoolGradeId(),
            valueOf(grade.getAttachedAt()),
            grade.getAttachedBy()
        );
    }

    public static List<QuestionBankGradeDto> toDtoList(List<QuestionBankGrade> grades) {
        return grades.stream()
            .map(QuestionBankGradeDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime date) {
        return date == null ? null : date.toString();
    }
}
