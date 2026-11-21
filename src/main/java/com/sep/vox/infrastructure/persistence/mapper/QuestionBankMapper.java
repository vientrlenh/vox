package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionBankJpaEntity;

public final class QuestionBankMapper {

    public static QuestionBank toDomain(QuestionBankJpaEntity jpa) {
        return QuestionBank.builder()
                .id(jpa.getId())
                .languageId(jpa.getLanguageId())
                .schoolId(jpa.getSchoolId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .ownerType(QuestionBankOwnerType.valueOf(jpa.getOwnerType()))
                .status(QuestionBankStatus.valueOf(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static QuestionBankJpaEntity toJpa(QuestionBank domain) {
        return QuestionBankJpaEntity.builder()
                .id(domain.getId())
                .languageId(domain.getLanguageId())
                .schoolId(domain.getSchoolId())
                .code(domain.getCode())
                .name(domain.getName())
                .description(domain.getDescription())
                .ownerType(domain.getOwnerType().name())
                .status(domain.getStatus().name())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
}
