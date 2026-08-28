package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.application.port.input.command.UpdateSupportedLanguageCommand;
import com.sep.vox.application.port.input.query.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.UpdateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.application.response.input.supportedlanguage.UpdateSupportedLanguageResponse;
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
        var updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
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
        var updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
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
        var updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
        var id = UUID.randomUUID();
        var expected = new SupportedLanguageDto(id, "EN", "English", null, false, null, null);
        when(detailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id))).thenReturn(expected);

        var result = controller.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id));
    }

    @Test
    void supported_language_should_use_active_only_for_school_admin() {
        authenticate("ROLE_SCHOOL_ADMIN");
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
        var id = UUID.randomUUID();
        var expected = new SupportedLanguageDto(id, "EN", "English", null, true, null, null);
        when(detailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id))).thenReturn(expected);

        var result = controller.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id));
    }

    @Test
    void update_supported_language_should_map_input_presence_and_return_response() {
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
        var id = UUID.randomUUID();
        var input = new HashMap<String, Object>();
        input.put("code", "fr");
        input.put("name", "French");
        input.put("description", null);
        input.put("isActive", false);
        var command = new UpdateSupportedLanguageCommand(
            id,
            "fr",
            true,
            "French",
            true,
            null,
            true,
            false,
            true
        );
        var expected = new UpdateSupportedLanguageResponse(id);
        when(updateUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSupportedLanguage(id, input);

        assertThat(result).isEqualTo(expected);
        verify(updateUseCase).execute(command);
    }

    @Test
    void update_supported_language_should_keep_absent_fields_unprovided() {
        var listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        var detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        var updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
        var id = UUID.randomUUID();
        var input = Map.<String, Object>of("name", "French");
        var command = new UpdateSupportedLanguageCommand(
            id,
            null,
            false,
            "French",
            true,
            null,
            false,
            null,
            false
        );
        var expected = new UpdateSupportedLanguageResponse(id);
        when(updateUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSupportedLanguage(id, input);

        assertThat(result).isEqualTo(expected);
        verify(updateUseCase).execute(command);
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
