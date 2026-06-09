package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;

class SchoolControllerTests {

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var request = new CreateSchoolClassRequest(
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class"
        );
        var expectedCommand = new CreateSchoolClassCommand(
            schoolId,
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class"
        );
        when(createUseCase.execute(expectedCommand)).thenReturn(new CreateSchoolClassResponse(schoolClassId));

        var response = controller.create(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new CreateSchoolClassResponse(schoolClassId));
        verify(createUseCase).execute(expectedCommand);
    }

    @Test
    void createClassUser_should_return_created_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var schoolClassUserId = UUID.randomUUID();
        var request = new CreateSchoolClassUserRequest(userId);
        var expectedCommand = new CreateSchoolClassUserCommand(schoolId, classId, userId);
        var expected = new CreateSchoolClassUserResponse(schoolClassUserId);
        when(createClassUserUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.createClassUser(schoolId, classId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(createClassUserUseCase).execute(expectedCommand);
    }

    @Test
    void delete_should_return_ok_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var expected = new DeleteSchoolClassResponse(schoolClassId, "SOFT", "ARCHIVED", "2026-06-06T12:00:00Z");
        when(deleteUseCase.execute(new DeleteSchoolClassCommand(schoolId, schoolClassId))).thenReturn(expected);

        var response = controller.delete(schoolId, schoolClassId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(deleteUseCase).execute(new DeleteSchoolClassCommand(schoolId, schoolClassId));
    }

    @Test
    void createImportFileSession_should_return_preview_response() throws Exception {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var importSessionId = UUID.randomUUID();
        var expected = new PreviewSchoolClassImportResponse(
            importSessionId,
            "classes.csv",
            List.of("code", "name"),
            Map.of("code", "code", "name", "name"),
            List.of(Map.of("code", "ENG-01", "name", "English 01")),
            1L,
            "2026-06-09T10:00:00+07:00"
        );
        var file = new MockMultipartFile(
            "file",
            "classes.csv",
            "text/csv",
            "code,name\nENG-01,English 01\n".getBytes(StandardCharsets.UTF_8)
        );
        when(previewUseCase.execute(any(PreviewSchoolClassImportFromFileCommand.class))).thenReturn(expected);

        var response = controller.createImportFileSession(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewUseCase).execute(any(PreviewSchoolClassImportFromFileCommand.class));
    }

    @Test
    void acceptImportSession_should_return_accept_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var importSessionId = UUID.randomUUID();
        var request = new AcceptSchoolClassImportRequest(Map.of(
            "Mã lớp", "code",
            "Tên lớp", "name",
            "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode"
        ));
        var expected = new AcceptSchoolClassImportResponse(importSessionId, 2L, 1L, 1L, 0L, "COMPLETED");
        var expectedCommand = new AcceptSchoolClassImportCommand(schoolId, importSessionId, request.confirmedMapping());
        when(acceptUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.acceptImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(acceptUseCase).execute(expectedCommand);
    }

    @Test
    void createClassUserImportFileSession_should_return_preview_response() throws Exception {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var importSessionId = UUID.randomUUID();
        var expected = new PreviewSchoolClassUserImportResponse(
            importSessionId,
            "class-users.csv",
            List.of("email", "classCode"),
            Map.of("email", "email", "classCode", "classCode"),
            List.of(Map.of("email", "student@example.com", "classCode", "ENG-01")),
            1L,
            "2026-06-09T10:00:00+07:00"
        );
        var file = new MockMultipartFile(
            "file",
            "class-users.csv",
            "text/csv",
            "email,classCode\nstudent@example.com,ENG-01\n".getBytes(StandardCharsets.UTF_8)
        );
        when(previewClassUserUseCase.execute(any(PreviewSchoolClassUserImportFromFileCommand.class))).thenReturn(expected);

        var response = controller.createClassUserImportFileSession(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewClassUserUseCase).execute(any(PreviewSchoolClassUserImportFromFileCommand.class));
    }

    @Test
    void acceptClassUserImportSession_should_return_accept_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase, previewUseCase,
            acceptUseCase, previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var importSessionId = UUID.randomUUID();
        var request = new AcceptSchoolClassUserImportRequest(Map.of(
            "Email", "email",
            "Mã lớp", "classCode"
        ));
        var expected = new AcceptSchoolClassUserImportResponse(importSessionId, 2L, 1L, 1L, 0L, "COMPLETED");
        var expectedCommand = new AcceptSchoolClassUserImportCommand(schoolId, importSessionId, request.confirmedMapping());
        when(acceptClassUserUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.acceptClassUserImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(acceptClassUserUseCase).execute(expectedCommand);
    }
}
