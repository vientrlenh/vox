package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.language.LearningLanguage;
import com.sep.vox.domain.model.language.LearningLanguageStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.infrastructure.persistence.entity.LearningLanguageJpaEntity;

public class LearningLanguageMapper {
    
    private LearningLanguageMapper() {}

    public static LearningLanguage toDomain(LearningLanguageJpaEntity jpa) {
        return LearningLanguage.builder()
            .id(jpa.getId())
            .code(Code.from(jpa.getCode()))
            .name(Name.from(jpa.getName()))
            .description(jpa.getDescription())
            .status(LearningLanguageStatus.from(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .availableAt(jpa.getAvailableAt())
            .unavailableAt(jpa.getUnavailableAt())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static LearningLanguageJpaEntity toJpa(LearningLanguage language) {
        return LearningLanguageJpaEntity.builder()
            .id(language.getId())
            .code(Code.valueOf(language.getCode()))
            .name(Name.valueOf(language.getName()))
            .description(language.getDescription()) 
            .status(LearningLanguageStatus.value(language.getStatus()))
            .createdAt(language.getCreatedAt())
            .availableAt(language.getAvailableAt())
            .unavailableAt(language.getUnavailableAt())
            .updatedBy(language.getUpdatedBy())
            .build();
    }
}
