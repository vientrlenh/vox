package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionBankGrade;
import com.sep.vox.infrastructure.persistence.entity.QuestionBankGradeJpaEntity;

public final class QuestionBankGradeMapper {

    public static QuestionBankGrade toDomain(QuestionBankGradeJpaEntity jpa) {
        return new QuestionBankGrade(
            jpa.getId(),
            jpa.getQuestionBankId(),
            jpa.getSchoolGradeId(),
            jpa.getAttachedAt(),
            jpa.getAttachedBy()
        );
    }

    public static QuestionBankGradeJpaEntity toJpa(QuestionBankGrade domain) {
        return new QuestionBankGradeJpaEntity(
            domain.getId(),
            domain.getQuestionBankId(),
            domain.getSchoolGradeId(),
            domain.getAttachedAt(),
            domain.getAttachedBy()
        );
    }
}
