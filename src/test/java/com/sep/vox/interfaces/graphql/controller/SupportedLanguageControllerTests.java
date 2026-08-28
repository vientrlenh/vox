package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase;
    private ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase;
    private UpdateSupportedLanguageUseCase updateSupportedLanguageUseCase;
    private SupportedLanguageController supportedLanguageController;

    @BeforeEach
    void setup() {
        viewSupportedLanguagesUseCase = mock(ViewSupportedLanguagesUseCase.class);
        viewSupportedLanguageDetailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        updateSupportedLanguageUseCase = mock(UpdateSupportedLanguageUseCase.class);
        supportedLanguageController = new SupportedLanguageController(viewSupportedLanguagesUseCase, viewSupportedLanguageDetailsUseCase, updateSupportedLanguageUseCase);
    }

    @Test
    void supported_languages_should_keep_is_active_filter_for_system_admin() {
        var expected = new PageResult<SupportedLanguageDto>(List.of(), 1, 20, 0, 0);
        var query = new ViewSupportedLanguagesQuery(1, 20, "eng", false);
        when(viewSupportedLanguagesUseCase.execute(query)).thenReturn(expected);

        var result = supportedLanguageController.supportedLanguages(1, 20, "eng", false);

        assertThat(result).isEqualTo(expected);
        verify(viewSupportedLanguagesUseCase).execute(query);
    }

    @Test
    void supported_language_should_use_full_access_for_system_admin() {
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
        var id = UUID.randomUUID();
        var expected = new SupportedLanguageDto(id, "EN", "English", null, true, null, null);
        when(viewSupportedLanguageDetailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id))).thenReturn(expected);

        var result = supportedLanguageController.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(viewSupportedLanguageDetailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id));
    }

    @Test
    void update_supported_language_should_map_input_presence_and_return_response() {
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
        when(updateSupportedLanguageUseCase.execute(command)).thenReturn(expected);

        var result = supportedLanguageController.updateSupportedLanguage(id, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSupportedLanguageUseCase).execute(command);
    }

    @Test
    void update_supported_language_should_keep_absent_fields_unprovided() {
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
        when(updateSupportedLanguageUseCase.execute(command)).thenReturn(expected);

        var result = supportedLanguageController.updateSupportedLanguage(id, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSupportedLanguageUseCase).execute(command);
    }

}
