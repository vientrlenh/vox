package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

public final class QuestionBankMapper {

    public static QuestionBank toDomain(QuestionBankJpaEntity jpa) {
        return new QuestionBank(
            jpa.getId(),
            jpa.getLanguageId(),
            jpa.getSchoolId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            QuestionBankOwnerType.valueOf(jpa.getOwnerType()),
            QuestionBankStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static QuestionBankJpaEntity toJpa(QuestionBank domain) {
        return new QuestionBankJpaEntity(
            domain.getId(),
            domain.getLanguageId(),
            domain.getSchoolId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.getOwnerType().name(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}
