package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;
import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.supportedlanguage.UpdateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.query.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.application.response.input.supportedlanguage.UpdateSupportedLanguageResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.interfaces.graphql.mapper.UpdateSupportedLanguageCommandMapper;

@Controller("graphqlSupportedLanguageController")
public class SupportedLanguageController {

    private final ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase;
    private final ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase;
    private final UpdateSupportedLanguageUseCase updateSupportedLanguageUseCase;

    public SupportedLanguageController(
            ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase,
            ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase,
            UpdateSupportedLanguageUseCase updateSupportedLanguageUseCase) {
        this.viewSupportedLanguagesUseCase = viewSupportedLanguagesUseCase;
        this.viewSupportedLanguageDetailsUseCase = viewSupportedLanguageDetailsUseCase;
        this.updateSupportedLanguageUseCase = updateSupportedLanguageUseCase;
    }

    @QueryMapping(name = "supportedLanguages")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SupportedLanguageDto> supportedLanguages(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "isActive") Boolean isActive) {
        validatePageSize(page, size);
        return viewSupportedLanguagesUseCase.execute(new ViewSupportedLanguagesQuery(page, size, search, isActive));
    }

    @QueryMapping(name = "supportedLanguage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public SupportedLanguageDto supportedLanguage(@Argument(name = "id") UUID id) {
        return viewSupportedLanguageDetailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id));
    }

    @MutationMapping(name = "updateSupportedLanguage")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UpdateSupportedLanguageResponse updateSupportedLanguage(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") Map<String, Object> input) {
        var command = UpdateSupportedLanguageCommandMapper.fromInput(id, input);
        return updateSupportedLanguageUseCase.execute(command);
    }

    private void validatePageSize(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new IllegalArgumentException("Số trang yêu cầu không hợp lệ");
        }
        if (size == null || size <= 0) {
            throw new IllegalArgumentException("Kích cỡ trang yêu cầu không hợp lệ");
        }
    }
}
