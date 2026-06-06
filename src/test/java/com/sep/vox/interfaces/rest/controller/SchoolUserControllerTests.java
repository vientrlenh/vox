package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.ImportSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ImportSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UploadSchoolUserImportFileUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportUploadResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.request.ImportFieldMappingRequest;
import com.sep.vox.interfaces.rest.dto.request.SchoolUserImportRequest;
import com.sep.vox.interfaces.rest.mapper.SchoolUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UploadSchoolUserImportFileCommandMapper;

import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

public class SchoolUserControllerTests {

    private CreateSchoolUserUseCase createSchoolUserUseCase;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private UploadSchoolUserImportFileUseCase uploadSchoolUserImportFileUseCase;
    private PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private ImportSchoolUsersUseCase importSchoolUsersUseCase;
    private SchoolUserController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createSchoolUserUseCase = mock(CreateSchoolUserUseCase.class);
        deleteSchoolUserUseCase = mock(DeleteSchoolUserUseCase.class);
        changeSchoolUserRoleUseCase = mock(ChangeSchoolUserRoleUseCase.class);
        uploadSchoolUserImportFileUseCase = mock(UploadSchoolUserImportFileUseCase.class);
        previewSchoolUserImportFromFileUseCase = mock(PreviewSchoolUserImportFromFileUseCase.class);
        importSchoolUsersUseCase = mock(ImportSchoolUsersUseCase.class);

        controller = new SchoolUserController(
            createSchoolUserUseCase,
            deleteSchoolUserUseCase,
            changeSchoolUserRoleUseCase,
            uploadSchoolUserImportFileUseCase,
            previewSchoolUserImportFromFileUseCase,
            importSchoolUsersUseCase
        );
    }

    @Test
    void create_user_should_return_created_with_response() {
        var request = new CreateSchoolUserRequest(
            "student@school.edu.vn", "0987654321", "John Cena",
            "2005-01-15", "123 Street", "STUDENT", null
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
    void upload_import_file_should_return_ok() {
        var file = new MockMultipartFile("file", "students.csv", "text/csv", "email".getBytes());
        var uploadResponse = new SchoolUserImportUploadResponse(
            "file-1",
            "students.csv",
            "CSV",
            5,
            OffsetDateTime.now()
        );
        when(uploadSchoolUserImportFileUseCase.execute(any())).thenReturn(uploadResponse);

        var response = controller.uploadImportFile(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(uploadResponse);

        var captor = ArgumentCaptor.forClass(com.sep.vox.application.port.input.command.UploadSchoolUserImportFileCommand.class);
        verify(uploadSchoolUserImportFileUseCase).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(UploadSchoolUserImportFileCommandMapper.fromRequest(schoolId, file));
    }

    @Test
    void import_users_should_return_ok() {
        var request = new SchoolUserImportRequest(
            "file-1",
            false,
            "STUDENT",
            java.util.Map.of(
                "email", new ImportFieldMappingRequest("Email", null, java.util.List.of("E-mail"), null, null),
                "phone", new ImportFieldMappingRequest("Phone", null, java.util.List.of("Số điện thoại"), null, null),
                "fullName", new ImportFieldMappingRequest("Full Name", null, java.util.List.of("Họ và tên", "Tên"), null, null),
                "dateOfBirth", new ImportFieldMappingRequest("DOB", null, java.util.List.of("Ngày sinh"), null, null)
            )
        );
        var expectedResponse = new SchoolUserImportResponse(
            "file-1",
            false,
            1,
            1,
            1,
            0,
            0,
            java.util.List.of(),
            java.util.List.of(userId)
        );
        when(importSchoolUsersUseCase.execute(any(ImportSchoolUsersCommand.class))).thenReturn(expectedResponse);

        var response = controller.importUsers(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expectedResponse);
        verify(importSchoolUsersUseCase).execute(SchoolUserImportCommandMapper.fromRequest(schoolId, request));
    }

    private SchoolUserResponse schoolUserResponse(UUID id, String roleCode, String studentId) {
        return new SchoolUserResponse(
            id, "user@school.edu.vn", "0987654321", "John Cena",
            roleCode, "INACTIVE", schoolId, studentId, OffsetDateTime.now()
        );
    }
}
