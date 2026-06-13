package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.application.port.input.query.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;

class SupportedLanguageControllerTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supported_languages_should_keep_is_active_filter_for_system_admin() {
        authenticate("ROLE_SYSTEM_ADMIN");
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase);
        var expected = new PageResult<SupportedLanguageDto>(List.of(), 1, 20, 0, 0);
        var query = new ViewSupportedLanguagesQuery(1, 20, "eng", false);
        when(listUseCase.execute(query)).thenReturn(expected);

        var result = controller.supportedLanguages(1, 20, "eng", false);

        assertThat(result).isEqualTo(expected);
        verify(listUseCase).execute(query);
    }

    @Test
    void supported_languages_should_override_active_filter_for_school_admin() {
        authenticate("ROLE_SCHOOL_ADMIN");
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase);
        var expected = new PageResult<SupportedLanguageDto>(List.of(), 1, 20, 0, 0);
        var query = new ViewSupportedLanguagesQuery(1, 20, "eng", true);
        when(listUseCase.execute(query)).thenReturn(expected);

        var result = controller.supportedLanguages(1, 20, "eng", false);

        assertThat(result).isEqualTo(expected);
        verify(listUseCase).execute(query);
    }

    @Test
    void supported_language_should_use_full_access_for_system_admin() {
        authenticate("ROLE_SYSTEM_ADMIN");
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase);
        var id = UUID.randomUUID();
        var expected = new SupportedLanguageDto(id, "EN", "English", null, false, null, null);
        when(detailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id, false))).thenReturn(expected);

        var result = controller.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id, false));
    }

    @Test
    void supported_language_should_use_active_only_for_school_admin() {
        authenticate("ROLE_SCHOOL_ADMIN");
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase);
        var id = UUID.randomUUID();
        var expected = new SupportedLanguageDto(id, "EN", "English", null, true, null, null);
        when(detailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id, true))).thenReturn(expected);

        var result = controller.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id, true));
    }

    private static void authenticate(String authority) {
        var auth = new UsernamePasswordAuthenticationToken(
            "user",
            null,
            List.of(new SimpleGrantedAuthority(authority))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
