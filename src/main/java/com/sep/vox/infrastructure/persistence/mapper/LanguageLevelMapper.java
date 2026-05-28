package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.languagelevel.LanguageLevel;
import com.sep.vox.domain.valueobject.LanguageRank;
import com.sep.vox.domain.valueobject.LevelCode;
import com.sep.vox.infrastructure.persistence.entity.LanguageLevelJpaEntity;

public final class LanguageLevelMapper {

    public static LanguageLevel toDomain(LanguageLevelJpaEntity jpa) {
        return new LanguageLevel(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getLanguageId(),
            new LevelCode(jpa.getCode()),
            jpa.getName(),
            new LanguageRank(jpa.getRank()),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static LanguageLevelJpaEntity toJpa(LanguageLevel languageLevel) {
        return new LanguageLevelJpaEntity(
            languageLevel.getId(),
            languageLevel.getSchoolId(),
            languageLevel.getLanguageId(),
            valueOf(languageLevel.getCode()),
            languageLevel.getName(),
            valueOf(languageLevel.getRank()),
            languageLevel.isActive(),
            languageLevel.getCreatedAt(),
            languageLevel.getUpdatedAt(),
            languageLevel.getCreatedBy(),
            languageLevel.getUpdatedBy()
        );
    }

    private static String valueOf(LevelCode code) {
        return code == null ? null : code.value();
    }

    private static int valueOf(LanguageRank rank) {
        return rank == null ? 0 : rank.value();
    }
}
