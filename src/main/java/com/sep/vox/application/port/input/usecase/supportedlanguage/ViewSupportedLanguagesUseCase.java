package com.sep.vox.application.port.input.usecase.supportedlanguage;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.repository.SupportedLanguageRepository;


@Service
public class ViewSupportedLanguagesUseCase implements IUseCase<ViewSupportedLanguagesQuery, PageResult<SupportedLanguageDto>> {

    private final SupportedLanguageRepository supportedLanguageRepository;

    public ViewSupportedLanguagesUseCase(SupportedLanguageRepository supportedLanguageRepository) {
        this.supportedLanguageRepository = supportedLanguageRepository;
    }

    @Override
    public PageResult<SupportedLanguageDto> execute(ViewSupportedLanguagesQuery input) {
        validatePage(input);
        var result = supportedLanguageRepository.findAll(
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.isActive(),
            input.page(), 
            input.size()
        );
        return SupportedLanguageDtoMapper.toDtoPage(result);
    }

    private void validatePage(ViewSupportedLanguagesQuery input) {
        if (input.page() <= 0 || input.size() <= 0) {
            throw new IllegalStateException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
    }
}
