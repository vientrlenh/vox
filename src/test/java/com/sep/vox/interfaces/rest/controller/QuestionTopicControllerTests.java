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

import com.sep.vox.application.port.input.command.CreateQuestionTopicCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.command.ReviewQuestionTopicCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.DeleteQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ReviewQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.application.response.input.questiontopic.UpdateQuestionTopicResponse;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicRequest;

class QuestionTopicControllerTests {

    @Test
    void create_should_return_created_response() {
        var useCase = mock(CreateQuestionTopicUseCase.class);
        var controller = controller(useCase);
        var bankId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var request = new CreateQuestionTopicRequest(bankId, "Speaking topic", "Topic description");
        var command = new CreateQuestionTopicCommand(bankId, "Speaking topic", "Topic description");
        var expected = new CreateQuestionTopicResponse(topicId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void update_should_return_ok_response() {
        var useCase = mock(UpdateQuestionTopicUseCase.class);
        var controller = controller(
            mock(CreateQuestionTopicUseCase.class),
            useCase,
            mock(DeleteQuestionTopicUseCase.class),
            mock(ReviewQuestionTopicUseCase.class)
        );
        var topicId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var request = new UpdateQuestionTopicRequest(bankId, "Updated topic", "Updated description");
        var command = new UpdateQuestionTopicCommand(topicId, bankId, "Updated topic", "Updated description");
        var expected = questionTopicDto(topicId, "UPDATED_TOPIC");
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.update(topicId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void delete_should_return_ok_response() {
        var useCase = mock(DeleteQuestionTopicUseCase.class);
        var controller = controller(
            mock(CreateQuestionTopicUseCase.class),
            mock(UpdateQuestionTopicUseCase.class),
            useCase,
            mock(ReviewQuestionTopicUseCase.class)
        );
        var topicId = UUID.randomUUID();
        var command = new DeleteQuestionTopicCommand(topicId);
        var expected = new UpdateQuestionTopicResponse(topicId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.delete(topicId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void review_action_should_return_ok_response() {
        var useCase = mock(ReviewQuestionTopicUseCase.class);
        var controller = controller(
            mock(CreateQuestionTopicUseCase.class),
            mock(UpdateQuestionTopicUseCase.class),
            mock(DeleteQuestionTopicUseCase.class),
            useCase
        );
        var topicId = UUID.randomUUID();
        var request = new ReviewQuestionTopicRequest("PUBLISHED");
        var command = new ReviewQuestionTopicCommand(topicId, QuestionTopicStatus.PUBLISHED);
        var expected = new UpdateQuestionTopicResponse(topicId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.reviewAction(topicId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void endpoints_should_use_expected_roles() throws Exception {
        assertRole("create", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", CreateQuestionTopicRequest.class);
        assertRole("update", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", UUID.class, UpdateQuestionTopicRequest.class);
        assertRole("delete", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", UUID.class);
        assertRole("reviewAction", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", UUID.class, ReviewQuestionTopicRequest.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionTopicController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionTopicController controller(CreateQuestionTopicUseCase createUseCase) {
        return controller(
            createUseCase,
            mock(UpdateQuestionTopicUseCase.class),
            mock(DeleteQuestionTopicUseCase.class),
            mock(ReviewQuestionTopicUseCase.class)
        );
    }

    private QuestionTopicController controller(
            CreateQuestionTopicUseCase createUseCase,
            UpdateQuestionTopicUseCase updateUseCase,
            DeleteQuestionTopicUseCase deleteUseCase,
            ReviewQuestionTopicUseCase reviewUseCase) {
        return new QuestionTopicController(createUseCase, updateUseCase, deleteUseCase, reviewUseCase);
    }

    private QuestionTopicDto questionTopicDto(UUID id, String code) {
        return new QuestionTopicDto(
            id,
            UUID.randomUUID(),
            code,
            code,
            code,
            "PUBLISHED",
            "2026-06-14T10:00:00Z",
            "2026-06-14T10:00:00Z"
        );
    }
}
