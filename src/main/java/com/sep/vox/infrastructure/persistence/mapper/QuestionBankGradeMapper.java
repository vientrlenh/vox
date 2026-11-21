package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionBankGrade;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionBankGradeJpaEntity;

public final class QuestionBankGradeMapper {

    public static QuestionBankGrade toDomain(QuestionBankGradeJpaEntity jpa) {
        return QuestionBankGrade.builder()
                .id(jpa.getId())
                .questionBankId(jpa.getQuestionBankId())
                .schoolGradeId(jpa.getSchoolGradeId())
                .attachedAt(jpa.getAttachedAt())
                .attachedBy(jpa.getAttachedBy())
                .build();
    }

    public static QuestionBankGradeJpaEntity toJpa(QuestionBankGrade domain) {
        return QuestionBankGradeJpaEntity.builder()
                .id(domain.getId())
                .questionBankId(domain.getQuestionBankId())
                .schoolGradeId(domain.getSchoolGradeId())
                .attachedAt(domain.getAttachedAt())
                .attachedBy(domain.getAttachedBy())
                .build();
    }
}
