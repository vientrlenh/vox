package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;

@Controller("graphqlSupportedLanguageController")
public class SupportedLanguageController {

    private static final String SYSTEM_ADMIN_AUTHORITY = "ROLE_SYSTEM_ADMIN";

    private final ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase;
    private final ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase;

    public SupportedLanguageController(
            ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase,
            ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase) {
        this.viewSupportedLanguagesUseCase = viewSupportedLanguagesUseCase;
        this.viewSupportedLanguageDetailsUseCase = viewSupportedLanguageDetailsUseCase;
    }

    @QueryMapping(name = "supportedLanguages")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public PageResult<SupportedLanguageDto> supportedLanguages(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "search") String search,
            @Argument(name = "isActive") Boolean isActive) {
        var effectiveIsActive = isSystemAdmin() ? isActive : Boolean.TRUE;
        return viewSupportedLanguagesUseCase.execute(new ViewSupportedLanguagesQuery(page, size, search, effectiveIsActive));
    }

    @QueryMapping(name = "supportedLanguage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public SupportedLanguageDto supportedLanguage(@Argument(name = "id") UUID id) {
        return viewSupportedLanguageDetailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id, !isSystemAdmin()));
    }

    private boolean isSystemAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> SYSTEM_ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }
}
