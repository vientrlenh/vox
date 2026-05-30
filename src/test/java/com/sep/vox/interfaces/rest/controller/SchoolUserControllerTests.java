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
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;

public class SchoolUserControllerTests {

    private CreateSchoolUserUseCase createSchoolUserUseCase;
    private ViewSchoolUserUseCase viewSchoolUserUseCase;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private SchoolUserController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createSchoolUserUseCase = mock(CreateSchoolUserUseCase.class);
        viewSchoolUserUseCase = mock(ViewSchoolUserUseCase.class);
        deleteSchoolUserUseCase = mock(DeleteSchoolUserUseCase.class);
        changeSchoolUserRoleUseCase = mock(ChangeSchoolUserRoleUseCase.class);
        controller = new SchoolUserController(
            createSchoolUserUseCase, viewSchoolUserUseCase,
            deleteSchoolUserUseCase, changeSchoolUserRoleUseCase
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
    void get_user_should_return_ok_with_response() {
        var expectedResponse = schoolUserResponse(userId, "TEACHER", null);
        when(viewSchoolUserUseCase.execute(any(ViewSchoolUserCommand.class))).thenReturn(expectedResponse);

        var response = controller.getUser(schoolId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Lấy thông tin người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expectedResponse);
        verify(viewSchoolUserUseCase).execute(new ViewSchoolUserCommand(schoolId, userId));
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

    private SchoolUserResponse schoolUserResponse(UUID id, String roleCode, String studentId) {
        return new SchoolUserResponse(
            id, "user@school.edu.vn", "0987654321", "John Cena",
            roleCode, "INACTIVE", schoolId, studentId, OffsetDateTime.now()
        );
    }
}
