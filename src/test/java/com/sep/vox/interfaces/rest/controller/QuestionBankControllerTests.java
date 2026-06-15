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

import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.command.ReviewQuestionBankCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ReviewQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.application.response.input.questionbank.UpdateQuestionBankResponse;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankRequest;

class QuestionBankControllerTests {

    @Test
    void create_system_should_return_created_response() {
        var useCase = mock(CreateSystemQuestionBankUseCase.class);
        var controller = controller(useCase);
        var languageId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var request = new CreateSystemQuestionBankRequest(languageId, "SYS_BANK", "System Bank", "System description");
        var command = new CreateSystemQuestionBankCommand(languageId, "SYS_BANK", "System Bank", "System description");
        var expected = new CreateQuestionBankResponse(bankId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.createSystem(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void create_school_should_return_created_response() {
        var useCase = mock(CreateSchoolQuestionBankUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankUseCase.class),
            useCase,
            mock(UpdateQuestionBankUseCase.class),
            mock(DeleteQuestionBankUseCase.class),
            mock(ReviewQuestionBankUseCase.class)
        );
        var languageId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var request = new CreateSchoolQuestionBankRequest(languageId, schoolId, "SCH_BANK", "School Bank", "School description");
        var command = new CreateSchoolQuestionBankCommand(languageId, schoolId, "SCH_BANK", "School Bank", "School description");
        var expected = new CreateQuestionBankResponse(bankId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.createSchool(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void update_should_return_ok_response() {
        var useCase = mock(UpdateQuestionBankUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankUseCase.class),
            mock(CreateSchoolQuestionBankUseCase.class),
            useCase,
            mock(DeleteQuestionBankUseCase.class),
            mock(ReviewQuestionBankUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var request = new UpdateQuestionBankRequest("Updated Bank", "Updated description", true);
        var command = new UpdateQuestionBankCommand(bankId, "Updated Bank", "Updated description", true);
        var expected = questionBankDto(bankId, "UPDATED_BANK");
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.update(bankId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void delete_should_return_ok_response() {
        var useCase = mock(DeleteQuestionBankUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankUseCase.class),
            mock(CreateSchoolQuestionBankUseCase.class),
            mock(UpdateQuestionBankUseCase.class),
            useCase,
            mock(ReviewQuestionBankUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var command = new DeleteQuestionBankCommand(bankId);
        var expected = new UpdateQuestionBankResponse(bankId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.delete(bankId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void review_action_should_return_ok_response() {
        var useCase = mock(ReviewQuestionBankUseCase.class);
        var controller = controller(
            mock(CreateSystemQuestionBankUseCase.class),
            mock(CreateSchoolQuestionBankUseCase.class),
            mock(UpdateQuestionBankUseCase.class),
            mock(DeleteQuestionBankUseCase.class),
            useCase
        );
        var bankId = UUID.randomUUID();
        var request = new ReviewQuestionBankRequest("PUBLISHED");
        var command = new ReviewQuestionBankCommand(bankId, QuestionBankStatus.PUBLISHED);
        var expected = new UpdateQuestionBankResponse(bankId);
        when(useCase.execute(command)).thenReturn(expected);

        var response = controller.reviewAction(bankId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(useCase).execute(command);
    }

    @Test
    void endpoints_should_use_expected_roles() throws Exception {
        assertRole("createSystem", "hasRole('SYSTEM_ADMIN')", CreateSystemQuestionBankRequest.class);
        assertRole("createSchool", "hasRole('SCHOOL_ADMIN')", CreateSchoolQuestionBankRequest.class);
        assertRole("update", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", UUID.class, UpdateQuestionBankRequest.class);
        assertRole("delete", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", UUID.class);
        assertRole("reviewAction", "hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')", UUID.class, ReviewQuestionBankRequest.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionBankController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionBankController controller(CreateSystemQuestionBankUseCase createSystemUseCase) {
        return controller(
            createSystemUseCase,
            mock(CreateSchoolQuestionBankUseCase.class),
            mock(UpdateQuestionBankUseCase.class),
            mock(DeleteQuestionBankUseCase.class),
            mock(ReviewQuestionBankUseCase.class)
        );
    }

    private QuestionBankController controller(
            CreateSystemQuestionBankUseCase createSystemUseCase,
            CreateSchoolQuestionBankUseCase createSchoolUseCase,
            UpdateQuestionBankUseCase updateUseCase,
            DeleteQuestionBankUseCase deleteUseCase,
            ReviewQuestionBankUseCase reviewUseCase) {
        return new QuestionBankController(
            createSystemUseCase,
            createSchoolUseCase,
            updateUseCase,
            deleteUseCase,
            reviewUseCase
        );
    }

    private QuestionBankDto questionBankDto(UUID id, String code) {
        return new QuestionBankDto(
            id,
            UUID.randomUUID(),
            code,
            code,
            code,
            true,
            "2026-06-14T10:00:00Z",
            "2026-06-14T10:00:00Z"
        );
    }
}
