package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetsUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetsRequest;

class QuestionAssetControllerTests {

    @Test
    void create_should_return_created_response() {
        var useCase = mock(CreateQuestionAssetsUseCase.class);
        var controller = controller(useCase, mock(DeleteQuestionAssetsUseCase.class), mock(UpdateQuestionAssetsUseCase.class));
        var questionId = UUID.randomUUID();
        var assetId = UUID.randomUUID();
        var request = request(assetId);
        var command = command(questionId, assetId);
        var expected = new UpdateQuestionResponse(questionId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.create(questionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void update_should_return_ok_response() {
        var useCase = mock(UpdateQuestionAssetsUseCase.class);
        var controller = controller(mock(CreateQuestionAssetsUseCase.class), mock(DeleteQuestionAssetsUseCase.class), useCase);
        var questionId = UUID.randomUUID();
        var assetId = UUID.randomUUID();
        var request = request(assetId);
        var command = command(questionId, assetId);
        var expected = new UpdateQuestionResponse(questionId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.update(questionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void delete_should_return_ok_response() {
        var useCase = mock(DeleteQuestionAssetsUseCase.class);
        var controller = controller(mock(CreateQuestionAssetsUseCase.class), useCase, mock(UpdateQuestionAssetsUseCase.class));
        var questionId = UUID.randomUUID();
        var expected = new UpdateQuestionResponse(questionId);
        when(useCase.execute(questionId)).thenReturn(expected);

        var response = controller.delete(questionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(questionId);
    }

    @Test
    void endpoints_should_use_expected_roles() throws Exception {
        assertRole("create", "hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')", UUID.class, UpdateQuestionAssetsRequest.class);
        assertRole("update", "hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')", UUID.class, UpdateQuestionAssetsRequest.class);
        assertRole("delete", "hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')", UUID.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionAssetController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionAssetController controller(
            CreateQuestionAssetsUseCase createUseCase,
            DeleteQuestionAssetsUseCase deleteUseCase,
            UpdateQuestionAssetsUseCase updateUseCase) {
        return new QuestionAssetController(createUseCase, deleteUseCase, updateUseCase);
    }

    private UpdateQuestionAssetsRequest request(UUID assetId) {
        return new UpdateQuestionAssetsRequest(List.of(
            new UpdateQuestionAssetsRequest.AssetItem(assetId, "Image", null, "Alt", "IMAGE", "https://vox.local/image.jpg", null, "desc", 1)
        ));
    }

    private UpdateQuestionAssetsCommand command(UUID questionId, UUID assetId) {
        return new UpdateQuestionAssetsCommand(questionId, List.of(
            new UpdateQuestionAssetsCommand.AssetItem(assetId, "Image", null, "Alt", "IMAGE", "https://vox.local/image.jpg", null, "desc", 1)
        ));
    }
}
