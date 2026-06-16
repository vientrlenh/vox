package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionEvaluationGuideUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionEvaluationGuideRequest;

class QuestionEvaluationGuideControllerTests {

    @Test
    void create_should_return_created_response() {
        var useCase = mock(CreateQuestionEvaluationGuideUseCase.class);
        var controller = controller(useCase, mock(DeleteQuestionEvaluationGuideUseCase.class), mock(UpdateQuestionEvaluationGuideUseCase.class));
        var questionId = UUID.randomUUID();
        var request = request();
        var command = command(questionId);
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
        var useCase = mock(UpdateQuestionEvaluationGuideUseCase.class);
        var controller = controller(mock(CreateQuestionEvaluationGuideUseCase.class), mock(DeleteQuestionEvaluationGuideUseCase.class), useCase);
        var questionId = UUID.randomUUID();
        var request = request();
        var command = command(questionId);
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
        var useCase = mock(DeleteQuestionEvaluationGuideUseCase.class);
        var controller = controller(mock(CreateQuestionEvaluationGuideUseCase.class), useCase, mock(UpdateQuestionEvaluationGuideUseCase.class));
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
        assertRole("create", "hasAnyRole('TEACHER', 'SYSTEM_ADMIN')", UUID.class, UpdateQuestionEvaluationGuideRequest.class);
        assertRole("update", "hasAnyRole('TEACHER', 'SYSTEM_ADMIN')", UUID.class, UpdateQuestionEvaluationGuideRequest.class);
        assertRole("delete", "hasAnyRole('TEACHER', 'SYSTEM_ADMIN')", UUID.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionEvaluationGuideController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionEvaluationGuideController controller(
            CreateQuestionEvaluationGuideUseCase createUseCase,
            DeleteQuestionEvaluationGuideUseCase deleteUseCase,
            UpdateQuestionEvaluationGuideUseCase updateUseCase) {
        return new QuestionEvaluationGuideController(createUseCase, deleteUseCase, updateUseCase);
    }

    private UpdateQuestionEvaluationGuideRequest request() {
        return new UpdateQuestionEvaluationGuideRequest("Expected", "Key", "Accept", "Off", "Hints", "Mistakes");
    }

    private UpdateQuestionEvaluationGuideCommand command(UUID questionId) {
        return new UpdateQuestionEvaluationGuideCommand(questionId, "Expected", "Key", "Accept", "Off", "Hints", "Mistakes");
    }
}
