package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.questionbank.QuestionBank;
import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

public final class QuestionBankMapper {

    public static QuestionBank toDomain(QuestionBankJpaEntity jpa) {
        return new QuestionBank(
            jpa.getId(),
            jpa.getBankName(),
            jpa.getDescription(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static QuestionBankJpaEntity toJpa(QuestionBank domain) {
        return new QuestionBankJpaEntity(
            domain.getId(),
            domain.getBankName(),
            domain.getDescription(),
            domain.isActive(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}
