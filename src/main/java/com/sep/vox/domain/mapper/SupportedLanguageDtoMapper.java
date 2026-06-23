package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.valueobject.LanguageCode;

public final class SupportedLanguageDtoMapper {

    private SupportedLanguageDtoMapper() {
    }

    public static SupportedLanguageDto toDto(SupportedLanguage language) {
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

    public static List<SupportedLanguageDto> toDtoList(List<SupportedLanguage> list) {
        return list.stream()
            .map(SupportedLanguageDtoMapper::toDto)
            .toList();
    }

    public static PageResult<SupportedLanguageDto> toDtoPage(PageResult<SupportedLanguage> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(LanguageCode code) {
        return code == null ? null : code.value();
    }
}
