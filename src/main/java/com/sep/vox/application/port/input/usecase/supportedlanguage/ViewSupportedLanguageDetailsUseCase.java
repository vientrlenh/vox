package com.sep.vox.application.port.input.usecase.supportedlanguage;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.supportedlanguage.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
@RequiredArgsConstructor
public class ViewSupportedLanguageDetailsUseCase implements IUseCase<ViewSupportedLanguageDetailsQuery, SupportedLanguageDto> {

    private final SupportedLanguageRepository supportedLanguageRepository;
    private final UserContextPort userContextPort;

    @Override
    public SupportedLanguageDto execute(ViewSupportedLanguageDetailsQuery input) {
        SupportedLanguage language = supportedLanguageRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Language not found"));
        if (!userContextPort.isSystemAdmin() && !language.isActive()) {
            throw new ResourceNotFoundException("Language not found");
        }
        return SupportedLanguageDtoMapper.toDto(language);
    }
}
