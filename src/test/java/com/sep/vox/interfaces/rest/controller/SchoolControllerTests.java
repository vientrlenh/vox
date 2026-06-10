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
import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptSchoolUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;

class SchoolControllerTests {

    private CreateSchoolClassUseCase createSchoolClassUseCase;
    private DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    private PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    private AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;
    private CreateSchoolUserUseCase createSchoolUserUseCase;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;
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
        changeSchoolUserRoleUseCase = mock(ChangeSchoolUserRoleUseCase.class);
        previewSchoolUserImportFromFileUseCase = mock(PreviewSchoolUserImportFromFileUseCase.class);
        acceptSchoolUserImportUseCase = mock(AcceptSchoolUserImportUseCase.class);

        controller = new SchoolController(
            createSchoolClassUseCase, deleteSchoolClassUseCase,
            previewSchoolClassImportFromFileUseCase, acceptSchoolClassImportUseCase,
            createSchoolUserUseCase, deleteSchoolUserUseCase,
            changeSchoolUserRoleUseCase, previewSchoolUserImportFromFileUseCase,
            acceptSchoolUserImportUseCase
        );
    }

    @Test
    void create_should_return_created_response() {
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
        when(createSchoolClassUseCase.execute(expectedCommand)).thenReturn(new CreateSchoolClassResponse(schoolClassId));

        var response = controller.create(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new CreateSchoolClassResponse(schoolClassId));
        verify(createSchoolClassUseCase).execute(expectedCommand);
    }

    @Test
    void delete_should_return_ok_response() {
        var expected = new DeleteSchoolClassResponse(schoolClassId, "SOFT", "ARCHIVED", "2026-06-06T12:00:00Z");
        when(deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(schoolId, schoolClassId))).thenReturn(expected);

        var response = controller.delete(schoolId, schoolClassId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(deleteSchoolClassUseCase).execute(new DeleteSchoolClassCommand(schoolId, schoolClassId));
    }

    @Test
    void createImportFileSession_should_return_preview_response() throws Exception {
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
        when(previewSchoolClassImportFromFileUseCase.execute(any(PreviewSchoolClassImportFromFileCommand.class))).thenReturn(expected);

        var response = controller.createImportFileSession(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewSchoolClassImportFromFileUseCase).execute(any(PreviewSchoolClassImportFromFileCommand.class));
    }

    @Test
    void acceptImportSession_should_return_accept_response() {
        var request = new AcceptSchoolClassImportRequest(Map.of(
            "Mã lớp", "code",
            "Tên lớp", "name",
            "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode"
        ));
        var expected = new AcceptSchoolClassImportResponse(importSessionId, 2L, 1L, 1L, 0L, "COMPLETED");
        var expectedCommand = new AcceptSchoolClassImportCommand(schoolId, importSessionId, request.confirmedMapping());
        when(acceptSchoolClassImportUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.acceptImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(acceptSchoolClassImportUseCase).execute(expectedCommand);
    }

    @Test
    void create_user_should_return_created_with_response() {
        var request = new CreateSchoolUserRequest(
            "student@school.edu.vn", "0987654321", "John Cena",
            "2005-01-15", "123 Street", "STUDENT", null, null, null
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
    void change_role_should_return_ok() {
        var request = new ChangeSchoolUserRoleRequest("TEACHER");

        var response = controller.changeUserRole(schoolId, userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Cập nhật vai trò người dùng thành công");
        verify(changeSchoolUserRoleUseCase).execute(new ChangeSchoolUserRoleCommand(schoolId, userId, "TEACHER"));
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

    private SchoolUserResponse schoolUserResponse(UUID id, String roleCode, String studentId) {
        return new SchoolUserResponse(id, schoolId, id, roleCode, studentId, null, null);
    }
}
