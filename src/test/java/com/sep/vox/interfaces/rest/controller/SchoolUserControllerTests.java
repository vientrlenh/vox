package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;

public class SchoolUserControllerTests {

    private CreateSchoolUserUseCase createSchoolUserUseCase;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;
    private SchoolUserController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createSchoolUserUseCase = mock(CreateSchoolUserUseCase.class);
        deleteSchoolUserUseCase = mock(DeleteSchoolUserUseCase.class);
        changeSchoolUserRoleUseCase = mock(ChangeSchoolUserRoleUseCase.class);
        previewSchoolUserImportFromFileUseCase = mock(PreviewSchoolUserImportFromFileUseCase.class);
        acceptSchoolUserImportUseCase = mock(AcceptSchoolUserImportUseCase.class);

        controller = new SchoolUserController(
            createSchoolUserUseCase,
            deleteSchoolUserUseCase,
            changeSchoolUserRoleUseCase,
            previewSchoolUserImportFromFileUseCase,
            acceptSchoolUserImportUseCase
        );
    }

    @Test
    void create_user_should_return_created_with_response() {
        var request = new CreateSchoolUserRequest(
            "student@school.edu.vn", "0987654321", "John Cena",
            "2005-01-15", "123 Street", "STUDENT", null, null, null
        );
        var expectedResponse = schoolUserResponse(userId, "STUDENT", null);
        when(createSchoolUserUseCase.execute(any(CreateSchoolUserCommand.class))).thenReturn(expectedResponse);

        var response = controller.createUser(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tạo người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expectedResponse);
        verify(createSchoolUserUseCase).execute(any(CreateSchoolUserCommand.class));
    }

    @Test
    void delete_user_should_return_ok() {
        var response = controller.deleteUser(schoolId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa người dùng thành công");
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
    void preview_import_should_return_ok() throws Exception {
        var file = new MockMultipartFile("file", "students.csv", "text/csv", "email,fullName\n".getBytes());
        var previewResponse = new PreviewSchoolUserImportResponse(
            sessionId, "students.csv",
            List.of("email", "fullName"),
            Map.of("email", "email"),
            List.of(),
            1L,
            OffsetDateTime.now().plusDays(1).toString()
        );
        when(previewSchoolUserImportFromFileUseCase.execute(any())).thenReturn(previewResponse);

        var response = controller.previewImportFile(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Preview import người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(previewResponse);
        verify(previewSchoolUserImportFromFileUseCase).execute(any());
    }

    @Test
    void accept_import_should_return_ok() {
        var request = new AcceptSchoolUserImportRequest(Map.of("Email", "email", "Họ tên", "fullName"));
        var acceptResponse = new AcceptSchoolUserImportResponse(sessionId, 2L, 2L, 0L, 0L, "COMPLETED");
        when(acceptSchoolUserImportUseCase.execute(any(AcceptSchoolUserImportCommand.class))).thenReturn(acceptResponse);

        var response = controller.acceptImportSession(schoolId, sessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Import người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(acceptResponse);
        verify(acceptSchoolUserImportUseCase).execute(any(AcceptSchoolUserImportCommand.class));
    }

    private SchoolUserResponse schoolUserResponse(UUID id, String roleCode, String studentId) {
        return new SchoolUserResponse(
            id, "user@school.edu.vn", "0987654321", "John Cena",
            roleCode, "INACTIVE", schoolId, studentId, OffsetDateTime.now(),
            UUID.randomUUID(), null, null
        );
    }
}
