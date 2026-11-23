package com.sep.vox.application.port.input.usecase.supportedlanguage;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.repository.SupportedLanguageRepository;


@Service
@RequiredArgsConstructor
public class ViewSupportedLanguagesUseCase implements IUseCase<ViewSupportedLanguagesQuery, PageResult<SupportedLanguageDto>> {

    private final SupportedLanguageRepository supportedLanguageRepository;
    private final UserContextPort userContextPort;

    @Override
    public PageResult<SupportedLanguageDto> execute(ViewSupportedLanguagesQuery input) {
        Boolean effectiveIsActive = userContextPort.isSystemAdmin() ? input.isActive() : Boolean.TRUE;
        PageResult<SupportedLanguage> result = supportedLanguageRepository.findAll(
            StringNormalization.trimAndCollapseSpaces(input.search()),
            effectiveIsActive,
            input.page(), 
            input.size()
        );
        return SupportedLanguageDtoMapper.toDtoPage(result);
    }
}
