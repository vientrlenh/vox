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

import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionCommand;
import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionContentCommand;
import com.sep.vox.application.port.input.usecase.question.CreateSchoolQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.ReviewQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionContentUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.application.response.input.question.DeleteQuestionResponse;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionContentRequest;

class QuestionControllerTests {

    @Test
    void create_system_should_return_created_response() {
        var useCase = mock(CreateSystemQuestionBankQuestionUseCase.class);
        var controller = controller(useCase);
        var topicId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var request = createQuestionRequest(topicId, "SYS_Q");
        var command = new CreateSystemQuestionBankQuestionCommand(
            topicId, "SYS_Q", "Instruction", "Question text", "Prompt", "Preparation", "SHORT_ANSWER", "QUESTION_BANK", "BANK_VISIBLE", 15, 30, 60);
        var expected = new CreateQuestionResponse(questionId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.createSystem(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void create_school_should_return_created_response() {
        var useCase = mock(CreateSchoolQuestionBankQuestionUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankQuestionUseCase.class),
            useCase,
            mock(DeleteQuestionUseCase.class),
            mock(UpdateQuestionContentUseCase.class),
            mock(ReviewQuestionUseCase.class)
        );
        var topicId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var request = createQuestionRequest(topicId, "SCH_Q");
        var command = new CreateSchoolQuestionBankQuestionCommand(
            topicId, "SCH_Q", "Instruction", "Question text", "Prompt", "Preparation", "SHORT_ANSWER", "QUESTION_BANK", "BANK_VISIBLE", 15, 30, 60);
        var expected = new CreateQuestionResponse(questionId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.createSchool(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void update_content_should_return_ok_response() {
        var useCase = mock(UpdateQuestionContentUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankQuestionUseCase.class),
            mock(CreateSchoolQuestionBankQuestionUseCase.class),
            mock(DeleteQuestionUseCase.class),
            useCase,
            mock(ReviewQuestionUseCase.class)
        );
        var questionId = UUID.randomUUID();
        var request = new UpdateQuestionContentRequest("Instruction", "Updated question", "Prompt", "Preparation", "SHORT_ANSWER", "QUESTION_BANK", "BANK_VISIBLE", 10, 20, 40);
        var command = new UpdateQuestionContentCommand(questionId, "Instruction", "Updated question", "Prompt", "Preparation", "SHORT_ANSWER", "QUESTION_BANK", "BANK_VISIBLE", 10, 20, 40);
        var expected = new UpdateQuestionResponse(questionId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.updateContent(questionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void delete_should_return_ok_response() {
        var useCase = mock(DeleteQuestionUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankQuestionUseCase.class),
            mock(CreateSchoolQuestionBankQuestionUseCase.class),
            useCase,
            mock(UpdateQuestionContentUseCase.class),
            mock(ReviewQuestionUseCase.class)
        );
        var questionId = UUID.randomUUID();
        var command = new DeleteQuestionCommand(questionId);
        var expected = new DeleteQuestionResponse(questionId, "HARD_DELETE", null);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.delete(questionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void review_action_should_return_ok_response() {
        var useCase = mock(ReviewQuestionUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankQuestionUseCase.class),
            mock(CreateSchoolQuestionBankQuestionUseCase.class),
            mock(DeleteQuestionUseCase.class),
            mock(UpdateQuestionContentUseCase.class),
            useCase
        );
        var questionId = UUID.randomUUID();
        var request = new ReviewQuestionRequest("APPROVED", "Looks good", "Ready");
        var command = new ReviewQuestionCommand(questionId, QuestionStatus.APPROVED, "Looks good", "Ready");
        var expected = new UpdateQuestionResponse(questionId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.reviewAction(questionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void endpoints_should_use_expected_roles() throws Exception {
        assertRole("createSystem", "hasRole('SYSTEM_ADMIN')", CreateSystemQuestionBankQuestionRequest.class);
        assertRole("createSchool", "hasRole('TEACHER')", CreateSystemQuestionBankQuestionRequest.class);
        assertRole("updateContent", "hasAnyRole('TEACHER', 'SYSTEM_ADMIN')", UUID.class, UpdateQuestionContentRequest.class);
        assertRole("delete", "hasAnyRole('TEACHER', 'SYSTEM_ADMIN')", UUID.class);
        assertRole("reviewAction", "hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')", UUID.class, ReviewQuestionRequest.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionController controller(CreateSystemQuestionBankQuestionUseCase createSystemUseCase) {
        return controller(
            createSystemUseCase,
            mock(CreateSchoolQuestionBankQuestionUseCase.class),
            mock(DeleteQuestionUseCase.class),
            mock(UpdateQuestionContentUseCase.class),
            mock(ReviewQuestionUseCase.class)
        );
    }

    private QuestionController controller(
            CreateSystemQuestionBankQuestionUseCase createSystemUseCase,
            CreateSchoolQuestionBankQuestionUseCase createSchoolUseCase,
            DeleteQuestionUseCase deleteUseCase,
            UpdateQuestionContentUseCase updateContentUseCase,
            ReviewQuestionUseCase reviewUseCase) {
        return new QuestionController(
            createSystemUseCase,
            createSchoolUseCase,
            deleteUseCase,
            updateContentUseCase,
            reviewUseCase
        );
    }

    private CreateSystemQuestionBankQuestionRequest createQuestionRequest(UUID topicId, String code) {
        return new CreateSystemQuestionBankQuestionRequest(
            topicId,
            code,
            "Instruction",
            "Question text",
            "Prompt",
            "Preparation",
            "SHORT_ANSWER",
            "QUESTION_BANK",
            "BANK_VISIBLE",
            15,
            30,
            60
        );
    }
}
