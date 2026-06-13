package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSupportedLanguageRequest;

class SupportedLanguageControllerTests {

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(createUseCase);
        var supportedLanguageId = UUID.randomUUID();
        var request = new CreateSupportedLanguageRequest("EN", "English", "Main language");
        var expectedCommand = new CreateSupportedLanguageCommand("EN", "English", "Main language");
        var expected = new CreateSupportedLanguageResponse(supportedLanguageId);
        when(createUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tạo Ngôn Ngữ Thành Công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(createUseCase).execute(expectedCommand);
    }
}
