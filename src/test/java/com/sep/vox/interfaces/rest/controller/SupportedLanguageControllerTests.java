package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.application.port.input.command.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.DeleteSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSupportedLanguageRequest;

class SupportedLanguageControllerTests {

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSupportedLanguageUseCase.class);
        var deleteUseCase = mock(DeleteSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(createUseCase, deleteUseCase);
        var supportedLanguageId = UUID.randomUUID();
        var request = new CreateSupportedLanguageRequest("EN", "English", "Main language");
        var expectedCommand = new CreateSupportedLanguageCommand("EN", "English", "Main language");
        var expected = new CreateSupportedLanguageResponse(supportedLanguageId);
        when(createUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tạo ngôn ngữ thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(createUseCase).execute(expectedCommand);
    }

    @Test
    void delete_should_return_ok_response() {
        var createUseCase = mock(CreateSupportedLanguageUseCase.class);
        var deleteUseCase = mock(DeleteSupportedLanguageUseCase.class);
        var controller = new SupportedLanguageController(createUseCase, deleteUseCase);
        var id = UUID.randomUUID();

        var response = controller.delete(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa ngôn ngữ thành công");
        assertThat(response.getBody().data()).isNull();
        verify(deleteUseCase).execute(new DeleteSupportedLanguageCommand(id));
    }

    @Test
    void delete_should_be_system_admin_only() throws NoSuchMethodException {
        var method = SupportedLanguageController.class.getMethod("delete", UUID.class);
        var annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('SYSTEM_ADMIN')");
    }
}
