package com.sep.vox.application.port.input.usecase.supportedlanguage;

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
public class ViewSupportedLanguagesUseCase implements IUseCase<ViewSupportedLanguagesQuery, PageResult<SupportedLanguageDto>> {

    private final SupportedLanguageRepository supportedLanguageRepository;
    private final UserContextPort userContextPort;

    public ViewSupportedLanguagesUseCase(SupportedLanguageRepository supportedLanguageRepository, UserContextPort userContextPort) {
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public PageResult<SupportedLanguageDto> execute(ViewSupportedLanguagesQuery input) {
        if (!userContextPort.isSystemAdmin() && input.isActive() != null && input.isActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException("Yêu cầu trạng thái để xem ngôn ngữ hỗ trợ không hợp lệ");
        }
        var result = supportedLanguageRepository.findAll(
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.isActive(),
            input.page(), 
            input.size()
        );
        return SupportedLanguageDtoMapper.toDtoPage(result);
    }
}
