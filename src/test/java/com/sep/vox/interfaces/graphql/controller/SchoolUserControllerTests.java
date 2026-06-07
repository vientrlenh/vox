package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

public class SchoolUserControllerTests {

    @Test
    void school_users_should_return_page_from_use_case() {
        var listSchoolUsersUseCase = mock(ListSchoolUsersUseCase.class);
        var viewSchoolUserUseCase = mock(ViewSchoolUserUseCase.class);
        var controller = new SchoolUserController(listSchoolUsersUseCase, viewSchoolUserUseCase, mock(UserRepository.class), mock(SchoolRepository.class));
        var schoolId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var response = schoolUserResponse(userId, "STUDENT", "STU-001", schoolId);
        var page = new PageResult<>(List.of(response), 1, 20, 1, 1);

        when(listSchoolUsersUseCase.execute(new ListSchoolUsersCommand(schoolId, 1, 20))).thenReturn(page);

        var result = controller.schoolUsers(schoolId, 1, 20);

        assertThat(result).isEqualTo(page);
        assertThat(result.content()).containsExactly(response);
        verify(listSchoolUsersUseCase).execute(new ListSchoolUsersCommand(schoolId, 1, 20));
    }

    @Test
    void school_user_should_return_details_from_use_case() {
        var listSchoolUsersUseCase = mock(ListSchoolUsersUseCase.class);
        var viewSchoolUserUseCase = mock(ViewSchoolUserUseCase.class);
        var controller = new SchoolUserController(listSchoolUsersUseCase, viewSchoolUserUseCase, mock(UserRepository.class), mock(SchoolRepository.class));
        var schoolId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var response = schoolUserResponse(userId, "TEACHER", null, schoolId);

        when(viewSchoolUserUseCase.execute(new ViewSchoolUserCommand(schoolId, userId))).thenReturn(response);

        var result = controller.schoolUser(schoolId, userId);

        assertThat(result).isEqualTo(response);
        assertThat(result.id()).isEqualTo(userId);
        verify(viewSchoolUserUseCase).execute(new ViewSchoolUserCommand(schoolId, userId));
    }

    @Test
    void school_users_should_reject_invalid_paging() {
        var listSchoolUsersUseCase = mock(ListSchoolUsersUseCase.class);
        var viewSchoolUserUseCase = mock(ViewSchoolUserUseCase.class);
        var controller = new SchoolUserController(listSchoolUsersUseCase, viewSchoolUserUseCase, mock(UserRepository.class), mock(SchoolRepository.class));

        assertThatThrownBy(() -> controller.schoolUsers(UUID.randomUUID(), 0, 20))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
    }

    private SchoolUserResponse schoolUserResponse(UUID id, String roleCode, String studentId, UUID schoolId) {
        return new SchoolUserResponse(
            id, "user@school.edu.vn", "0987654321", "John Cena",
            roleCode, "INACTIVE", schoolId, studentId, OffsetDateTime.now(),
            UUID.randomUUID(), null, null
        );
    }
}