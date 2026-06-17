package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.valueobject.LanguageCode;

public final class SupportedLanguageDtoMapper {

    public static SupportedLanguageDto toSupportedLanguageDto(SupportedLanguage language) {
        return new SupportedLanguageDto(
            language.getId(),
            valueOf(language.getCode()),
            language.getName(),
            language.getDescription(),
            language.isActive(),
            language.getCreatedAt(),
            language.getUpdatedAt()
        );
    }

    private static String valueOf(LanguageCode code) {
        return code == null ? null : code.value();
    }
}
