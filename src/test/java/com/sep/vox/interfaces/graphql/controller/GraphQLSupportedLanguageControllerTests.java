package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.supportedlanguage.UpdateSupportedLanguageCommand;
import com.sep.vox.application.port.input.query.supportedlanguage.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.query.supportedlanguage.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.UpdateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.application.response.input.supportedlanguage.UpdateSupportedLanguageResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.interfaces.graphql.input.supportedlanguage.UpdateSupportedLanguageInput;

class GraphQLSupportedLanguageControllerTests {

    private ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase;
    private ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase;
    private UpdateSupportedLanguageUseCase updateSupportedLanguageUseCase;
    private GraphQLSupportedLanguageController supportedLanguageController;

    @BeforeEach
    void setup() {
        viewSupportedLanguagesUseCase = mock(ViewSupportedLanguagesUseCase.class);
        viewSupportedLanguageDetailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        updateSupportedLanguageUseCase = mock(UpdateSupportedLanguageUseCase.class);
        supportedLanguageController = new GraphQLSupportedLanguageController(viewSupportedLanguagesUseCase, viewSupportedLanguageDetailsUseCase, updateSupportedLanguageUseCase);
    }

    @Test
    void supported_languages_should_keep_is_active_filter_for_system_admin() {
        PageResult<SupportedLanguageDto> expected = new PageResult<SupportedLanguageDto>(List.of(), 1, 20, 0, 0);
        ViewSupportedLanguagesQuery query = new ViewSupportedLanguagesQuery(1, 20, "eng", false);
        when(viewSupportedLanguagesUseCase.execute(query)).thenReturn(expected);

        PageResult<SupportedLanguageDto> result = supportedLanguageController.supportedLanguages(1, 20, "eng", false);

        assertThat(result).isEqualTo(expected);
        verify(viewSupportedLanguagesUseCase).execute(query);
    }

    @Test
    void supported_language_should_use_full_access_for_system_admin() {
        ViewSupportedLanguagesUseCase listUseCase = mock(ViewSupportedLanguagesUseCase.class);
        ViewSupportedLanguageDetailsUseCase detailsUseCase = mock(ViewSupportedLanguageDetailsUseCase.class);
        UpdateSupportedLanguageUseCase updateUseCase = mock(UpdateSupportedLanguageUseCase.class);
        GraphQLSupportedLanguageController controller = new GraphQLSupportedLanguageController(listUseCase, detailsUseCase, updateUseCase);
        UUID id = UUID.randomUUID();
        SupportedLanguageDto expected = new SupportedLanguageDto(id, "EN", "English", null, false, null, null);
        when(detailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id))).thenReturn(expected);

        SupportedLanguageDto result = controller.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id));
    }

    @Test
    void supported_language_should_use_active_only_for_school_admin() {
        UUID id = UUID.randomUUID();
        SupportedLanguageDto expected = new SupportedLanguageDto(id, "EN", "English", null, true, null, null);
        when(viewSupportedLanguageDetailsUseCase.execute(new ViewSupportedLanguageDetailsQuery(id))).thenReturn(expected);

        SupportedLanguageDto result = supportedLanguageController.supportedLanguage(id);

        assertThat(result).isEqualTo(expected);
        verify(viewSupportedLanguageDetailsUseCase).execute(new ViewSupportedLanguageDetailsQuery(id));
    }

    @Test
    void update_supported_language_should_map_input_presence_and_return_response() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageInput input = new UpdateSupportedLanguageInput(
            ArgumentValue.ofNullable("fr"), ArgumentValue.ofNullable("French"),
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(false));
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(
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
        UpdateSupportedLanguageResponse expected = new UpdateSupportedLanguageResponse(id);
        when(updateSupportedLanguageUseCase.execute(command)).thenReturn(expected);

        UpdateSupportedLanguageResponse result = supportedLanguageController.updateSupportedLanguage(id, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSupportedLanguageUseCase).execute(command);
    }

    @Test
    void update_supported_language_should_keep_absent_fields_unprovided() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageInput input = new UpdateSupportedLanguageInput(
            ArgumentValue.omitted(), ArgumentValue.ofNullable("French"),
            ArgumentValue.omitted(), ArgumentValue.omitted());
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(
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
        UpdateSupportedLanguageResponse expected = new UpdateSupportedLanguageResponse(id);
        when(updateSupportedLanguageUseCase.execute(command)).thenReturn(expected);

        UpdateSupportedLanguageResponse result = supportedLanguageController.updateSupportedLanguage(id, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSupportedLanguageUseCase).execute(command);
    }

}
