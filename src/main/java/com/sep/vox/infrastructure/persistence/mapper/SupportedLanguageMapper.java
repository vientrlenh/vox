package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.infrastructure.persistence.entity.SupportedLanguageJpaEntity;

public final class SupportedLanguageMapper {
    
    public static SupportedLanguage toDomain(SupportedLanguageJpaEntity jpa) {
        return new SupportedLanguage(
            jpa.getId(), 
            new LanguageCode(jpa.getCode()), 
            jpa.getName(), 
            jpa.getDescription(), 
            jpa.isActive(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static SupportedLanguageJpaEntity toJpa(SupportedLanguage supportedLanguage) {
        return new SupportedLanguageJpaEntity(
            supportedLanguage.getId(), 
            valueOf(supportedLanguage.getCode()), 
            supportedLanguage.getName(), 
            supportedLanguage.getDescription(), 
            supportedLanguage.isActive(), 
            supportedLanguage.getCreatedAt(), 
            supportedLanguage.getUpdatedAt(), 
            supportedLanguage.getCreatedBy(),
            supportedLanguage.getUpdatedBy()
        );
    }

    private static String valueOf(LanguageCode code) {
        return code == null ? null : code.value();
    }
}
