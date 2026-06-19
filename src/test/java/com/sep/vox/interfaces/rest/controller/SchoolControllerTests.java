package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptSchoolUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.application.port.input.usecase.schoolclassuser.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.DeleteSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.PreviewSchoolClassUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.UpdateSchoolClassUserStatusUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.schoolclassuser.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassUserStatusRequest;

class SchoolControllerTests {

    private CreateSchoolClassUseCase createSchoolClassUseCase;
    private DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    private PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    private AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;
    private CreateSchoolUserUseCase createSchoolUserUseCase;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;
    private CreateSchoolClassUserUseCase createSchoolClassUserUseCase;
    private DeleteSchoolClassUserUseCase deleteSchoolClassUserUseCase;
    private UpdateSchoolClassUserStatusUseCase updateSchoolClassUserStatusUseCase;
    private PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase;
    private AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase;
    private SchoolController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID schoolClassId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID importSessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        deleteSchoolClassUseCase = mock(DeleteSchoolClassUseCase.class);
        previewSchoolClassImportFromFileUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        acceptSchoolClassImportUseCase = mock(AcceptSchoolClassImportUseCase.class);
        createSchoolUserUseCase = mock(CreateSchoolUserUseCase.class);
        deleteSchoolUserUseCase = mock(DeleteSchoolUserUseCase.class);
        previewSchoolUserImportFromFileUseCase = mock(PreviewSchoolUserImportFromFileUseCase.class);
        acceptSchoolUserImportUseCase = mock(AcceptSchoolUserImportUseCase.class);
        createSchoolClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        deleteSchoolClassUserUseCase = mock(DeleteSchoolClassUserUseCase.class);
        updateSchoolClassUserStatusUseCase = mock(UpdateSchoolClassUserStatusUseCase.class);
        
        controller = new SchoolController(
            createSchoolClassUseCase, createSchoolClassUserUseCase,
            deleteSchoolClassUseCase, deleteSchoolClassUserUseCase,
            updateSchoolClassUserStatusUseCase,
            previewSchoolClassImportFromFileUseCase, acceptSchoolClassImportUseCase,
            createSchoolUserUseCase, deleteSchoolUserUseCase,
            previewSchoolUserImportFromFileUseCase,
            acceptSchoolUserImportUseCase,
            previewSchoolClassUserImportFromFileUseCase, acceptSchoolClassUserImportUseCase
        );
    }

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
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
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
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
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var response = controller.delete(schoolId, schoolClassId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa lớp học thành công");
        assertThat(response.getBody().data()).isNull();
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
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
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
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var importSessionId = UUID.randomUUID();
        var request = new AcceptSchoolClassImportRequest(Map.of(
            "Mã lớp", "code",
            "Tên lớp", "name",
            "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode"
        ));
        var expected = new AcceptSchoolClassImportResponse(importSessionId, 2L, 1L, 0L, 1L, 0L, "COMPLETED");
        var expectedCommand = new AcceptSchoolClassImportCommand(schoolId, importSessionId, request.confirmedMapping());
        when(acceptUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.acceptImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(acceptUseCase).execute(expectedCommand);
    }

    @Test
    void create_user_should_return_created_with_response() {
        var request = new CreateSchoolUserRequest(
            "student@school.edu.vn", "0987654321", "John Cena",
            "2005-01-15", "123 Street", "STUDENT", null, null
        );
        var expected = new CreateSchoolUserResponse(userId);
        when(createSchoolUserUseCase.execute(any(CreateSchoolUserCommand.class))).thenReturn(expected);

        var response = controller.createUser(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tạo người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(createSchoolUserUseCase).execute(any(CreateSchoolUserCommand.class));
    }

    @Test
    void delete_user_should_return_no_content() {
        var response = controller.deleteUser(schoolId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(deleteSchoolUserUseCase).execute(new DeleteSchoolUserCommand(schoolId, userId));
    }

    @Test
    void preview_import_user_should_return_ok() throws Exception {
        var file = new MockMultipartFile("file", "students.csv", "text/csv", "email,fullName\n".getBytes());
        var expected = new PreviewSchoolUserImportResponse(
            importSessionId, "students.csv",
            List.of("email", "fullName"),
            Map.of("email", "email"),
            List.of(), 1L,
            OffsetDateTime.now().plusDays(1).toString()
        );
        when(previewSchoolUserImportFromFileUseCase.execute(any())).thenReturn(expected);

        var response = controller.previewImportFile(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Preview import người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewSchoolUserImportFromFileUseCase).execute(any());
    }

    @Test
    void accept_import_user_should_return_ok() {
        var request = new AcceptSchoolUserImportRequest(Map.of("Email", "email", "Họ tên", "fullName"));
        var expected = new AcceptSchoolUserImportResponse(importSessionId, 2L, 2L, 0L, 0L, "COMPLETED");
        when(acceptSchoolUserImportUseCase.execute(any(AcceptSchoolUserImportCommand.class))).thenReturn(expected);

        var response = controller.acceptImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Import người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(acceptSchoolUserImportUseCase).execute(any(AcceptSchoolUserImportCommand.class));
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
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
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
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            mock(DeleteSchoolClassUserUseCase.class), mock(UpdateSchoolClassUserStatusUseCase.class), previewUseCase,
            acceptUseCase, mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
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

    @Test
    void deleteClassUser_should_return_ok_response_without_success_field() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var deleteClassUserUseCase = mock(DeleteSchoolClassUserUseCase.class);
        var updateClassUserStatusUseCase = mock(UpdateSchoolClassUserStatusUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            deleteClassUserUseCase, updateClassUserStatusUseCase, previewUseCase, acceptUseCase,
            mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var expectedCommand = new DeleteSchoolClassUserCommand(schoolId, classId, userId);
        var response = controller.deleteClassUser(schoolId, classId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa người dùng khỏi lớp học thành công");
        assertThat(response.getBody().data()).isNull();
        verify(deleteClassUserUseCase).execute(expectedCommand);
    }

    @Test
    void updateClassUserStatus_should_return_ok_response_without_success_field() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var createClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var deleteClassUserUseCase = mock(DeleteSchoolClassUserUseCase.class);
        var updateClassUserStatusUseCase = mock(UpdateSchoolClassUserStatusUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var previewClassUserUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        var acceptClassUserUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        var controller = new SchoolController(createUseCase, createClassUserUseCase, deleteUseCase,
            deleteClassUserUseCase, updateClassUserStatusUseCase, previewUseCase, acceptUseCase,
            mock(CreateSchoolUserUseCase.class), mock(DeleteSchoolUserUseCase.class),
            mock(PreviewSchoolUserImportFromFileUseCase.class),
            mock(AcceptSchoolUserImportUseCase.class), previewClassUserUseCase, acceptClassUserUseCase);
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var request = new UpdateSchoolClassUserStatusRequest(true);
        var expectedCommand = new UpdateSchoolClassUserStatusCommand(schoolId, classId, userId, true);
        var expected = new UpdateSchoolClassUserStatusResponse(classId);
        when(updateClassUserStatusUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.updateClassUserStatus(schoolId, classId, userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Cập nhật trạng thái người dùng trong lớp học thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(updateClassUserStatusUseCase).execute(expectedCommand);
    }
}
