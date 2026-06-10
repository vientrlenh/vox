package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

class SchoolControllerTests {

    private ViewSchoolsUseCase viewSchoolsUseCase;
    private ViewSchoolClassesUseCase viewSchoolClassesUseCase;
    private ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;
    private UpdateSchoolClassUseCase updateSchoolClassUseCase;
    private ListSchoolUsersUseCase listSchoolUsersUseCase;
    private ViewSchoolUserUseCase viewSchoolUserUseCase;
    private UpdateSchoolUserUseCase updateSchoolUserUseCase;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private SchoolController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewSchoolsUseCase = mock(ViewSchoolsUseCase.class);
        viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
        viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
        listSchoolUsersUseCase = mock(ListSchoolUsersUseCase.class);
        viewSchoolUserUseCase = mock(ViewSchoolUserUseCase.class);
        updateSchoolUserUseCase = mock(UpdateSchoolUserUseCase.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);

        controller = new SchoolController(
            viewSchoolsUseCase, viewSchoolClassesUseCase,
            viewSchoolClassDetailsUseCase, updateSchoolClassUseCase,
            listSchoolUsersUseCase, viewSchoolUserUseCase,
            updateSchoolUserUseCase,
            userRepository, schoolRepository
        );
    }

    @Test
    void school_classes_should_return_page_result() {
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var expected = new PageResult<SchoolClassResponse>(List.of(), 1, 20, 0, 0);
        var query = new ViewSchoolClassesQuery(1, 20, "eng", "ACTIVE", languageId, gradeId);
        when(viewSchoolClassesUseCase.execute(query)).thenReturn(expected);

        var result = controller.schoolClasses(1, 20, "eng", "ACTIVE", languageId, gradeId);

        assertThat(result).isEqualTo(expected);
        verify(viewSchoolClassesUseCase).execute(query);
    }

    @Test
    void school_classes_should_throw_when_page_or_size_invalid() {
        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(0, 20, null, null, null, null));
        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(1, 0, null, null, null, null));
    }

    @Test
    void school_class_should_return_details() {
        var classId = UUID.randomUUID();
        var expected = new SchoolClassResponse(
            classId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            "Starter class",
            "ACTIVE",
            "2026-06-06T12:00:00Z",
            "2026-06-06T12:00:00Z"
        );
        when(viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(classId))).thenReturn(expected);

        var result = controller.schoolClass(classId);

        assertThat(result).isEqualTo(expected);
        verify(viewSchoolClassDetailsUseCase).execute(new ViewSchoolClassDetailsQuery(classId));
    }

    @Test
    void update_school_class_name_only_should_return_id_response() {
        var classId = UUID.randomUUID();
        var input = Map.<String, Object>of("name", "English 02");
        var command = new UpdateSchoolClassCommand(classId, "English 02", true, null, false, null, false);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSchoolClassUseCase).execute(command);
    }

    @Test
    void update_school_class_description_null_should_map_presence() {
        var classId = UUID.randomUUID();
        var input = new HashMap<String, Object>();
        input.put("description", null);
        var command = new UpdateSchoolClassCommand(classId, null, false, null, true, null, false);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSchoolClassUseCase).execute(command);
    }

    @Test
    void update_school_class_status_only_should_map_presence() {
        var classId = UUID.randomUUID();
        var input = Map.<String, Object>of("status", "INACTIVE");
        var command = new UpdateSchoolClassCommand(classId, null, false, null, false, "INACTIVE", true);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSchoolClassUseCase).execute(command);
    }

    @Test
    void school_users_should_return_page_from_use_case() {
        var response = schoolUserResponse(userId, "STUDENT", "STU-001");
        var page = new PageResult<>(List.of(response), 1, 20, 1, 1);
        when(listSchoolUsersUseCase.execute(new ListSchoolUsersCommand(schoolId, 1, 20))).thenReturn(page);

        var result = controller.schoolUsers(schoolId, 1, 20);

        assertThat(result).isEqualTo(page);
        assertThat(result.content()).containsExactly(response);
        verify(listSchoolUsersUseCase).execute(new ListSchoolUsersCommand(schoolId, 1, 20));
    }

    @Test
    void school_user_should_return_details_from_use_case() {
        var response = schoolUserResponse(userId, "TEACHER", null);
        when(viewSchoolUserUseCase.execute(new ViewSchoolUserCommand(schoolId, userId))).thenReturn(response);

        var result = controller.schoolUser(schoolId, userId);

        assertThat(result).isEqualTo(response);
        assertThat(result.id()).isEqualTo(userId);
        verify(viewSchoolUserUseCase).execute(new ViewSchoolUserCommand(schoolId, userId));
    }

    @Test
    void school_users_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.schoolUsers(UUID.randomUUID(), 0, 20))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
    }

    private SchoolUserResponse schoolUserResponse(UUID id, String roleCode, String studentId) {
        return new SchoolUserResponse(id, schoolId, id, roleCode, studentId, null, null);
    }
}
