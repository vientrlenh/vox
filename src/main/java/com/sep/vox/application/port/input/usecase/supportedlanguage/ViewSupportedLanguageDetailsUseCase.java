package com.sep.vox.application.port.input.usecase.supportedlanguage;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
public class ViewSupportedLanguageDetailsUseCase implements IUseCase<ViewSupportedLanguageDetailsQuery, SupportedLanguageDto> {

    private final SupportedLanguageRepository supportedLanguageRepository;

    public ViewSupportedLanguageDetailsUseCase(SupportedLanguageRepository supportedLanguageRepository) {
        this.supportedLanguageRepository = supportedLanguageRepository;
    }

    @Override
    public SupportedLanguageDto execute(ViewSupportedLanguageDetailsQuery input) {
        var language = supportedLanguageRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngôn ngữ"));
        if (input.activeOnly() && !language.isActive()) {
            throw new NotFoundException("Không tìm thấy ngôn ngữ");
        }
        return SupportedLanguageDtoMapper.toDto(language);
    }
}
