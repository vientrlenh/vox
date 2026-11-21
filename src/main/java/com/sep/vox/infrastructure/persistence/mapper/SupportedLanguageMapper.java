package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.infrastructure.persistence.entity.supportedlanguage.SupportedLanguageJpaEntity;

public final class SupportedLanguageMapper {
    
    public static SupportedLanguage toDomain(SupportedLanguageJpaEntity jpa) {
        return SupportedLanguage.builder()
            .id(jpa.getId())
            .code(new LanguageCode(jpa.getCode()))
            .name(jpa.getName())
            .description(jpa.getDescription())
            .active(jpa.isActive())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static SupportedLanguageJpaEntity toJpa(SupportedLanguage supportedLanguage) {
        return SupportedLanguageJpaEntity.builder()
            .id(supportedLanguage.getId())
            .code(valueOf(supportedLanguage.getCode()))
            .name(supportedLanguage.getName())
            .description(supportedLanguage.getDescription())
            .isActive(supportedLanguage.isActive())
            .createdAt(supportedLanguage.getCreatedAt())
            .updatedAt(supportedLanguage.getUpdatedAt())
            .createdBy(supportedLanguage.getCreatedBy())
            .updatedBy(supportedLanguage.getUpdatedBy())
            .build();
    }

    private static String valueOf(LanguageCode code) {
        return code == null ? null : code.value();
    }
}
