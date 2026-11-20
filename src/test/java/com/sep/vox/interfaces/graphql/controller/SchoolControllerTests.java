package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesByUserQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolStudentsBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolTeachersBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.school.ListSchoolsWithOnGoingAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMySchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesByUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryCursorPageUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryPageUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.UpdateGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelDetailsUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelsUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradesUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolStudentsBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolTeachersBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersForRequesterUseCase;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;

import graphql.schema.DataFetchingEnvironment;

class SchoolControllerTests {
    private ViewSchoolsUseCase viewSchoolsUseCase;
    private ViewSchoolClassesUseCase viewSchoolClassesUseCase;
    private ViewSchoolClassesByUserUseCase viewSchoolClassesByUserUseCase;
    private ViewMySchoolClassesUseCase viewMySchoolClassesUseCase;
    private ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;
    private ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase;
    private UpdateSchoolClassUseCase updateSchoolClassUseCase;
    private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    private ViewSchoolUsersForRequesterUseCase viewSchoolUsersForRequesterUseCase;
    private ViewSchoolStudentsBySchoolUseCase viewSchoolStudentsBySchoolUseCase;
    private ViewSchoolTeachersBySchoolUseCase viewSchoolTeachersBySchoolUseCase;
    private ViewSchoolUserDetailsUseCase viewSchoolUserDetailsUseCase;
    private UpdateSchoolUserUseCase updateSchoolUserUseCase; 
    private UpdateSchoolUseCase updateSchoolUseCase;
    private ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    private UpdateSchoolRoomUseCase updateSchoolRoomUseCase;
    private UpdateSchoolGradeUseCase updateSchoolGradeUseCase;
    private ViewSchoolGradesUseCase viewSchoolGradesUseCase;
    private ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase;
    private ViewGradeLevelsUseCase viewGradeLevelsUseCase;
    private ViewGradeLevelDetailsUseCase viewGradeLevelDetailsUseCase;
    private UpdateGradeLevelUseCase updateGradeLevelUseCase;
    private ViewSchoolDirectoryCursorPageUseCase viewSchoolDirectoryCursorPageUseCase;
    private ViewSchoolDirectoryPageUseCase viewSchoolDirectoryPageUseCase;
    private ViewSchoolDirectoryDetailsUseCase viewSchoolDirectoryDetailsUseCase;
    private ListSchoolsWithOnGoingAssessmentUseCase listSchoolsWithOnGoingAssessmentUseCase;
    private SchoolController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewSchoolsUseCase = mock(ViewSchoolsUseCase.class);
        viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
        viewSchoolClassesByUserUseCase = mock(ViewSchoolClassesByUserUseCase.class);
        viewMySchoolClassesUseCase = mock(ViewMySchoolClassesUseCase.class);
        viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        viewSchoolClassUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
        viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
        viewSchoolUsersForRequesterUseCase = mock(ViewSchoolUsersForRequesterUseCase.class);
        viewSchoolStudentsBySchoolUseCase = mock(ViewSchoolStudentsBySchoolUseCase.class);
        viewSchoolTeachersBySchoolUseCase = mock(ViewSchoolTeachersBySchoolUseCase.class);
        viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
        updateSchoolUserUseCase = mock(UpdateSchoolUserUseCase.class);
        updateSchoolUseCase = mock(UpdateSchoolUseCase.class);
        viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        updateSchoolGradeUseCase = mock(UpdateSchoolGradeUseCase.class);
        viewSchoolGradesUseCase = mock(ViewSchoolGradesUseCase.class);
        viewSchoolGradeDetailsUseCase = mock(ViewSchoolGradeDetailsUseCase.class);
        viewGradeLevelsUseCase = mock(ViewGradeLevelsUseCase.class);
        viewGradeLevelDetailsUseCase = mock(ViewGradeLevelDetailsUseCase.class);
        updateGradeLevelUseCase = mock(UpdateGradeLevelUseCase.class);
        viewSchoolDirectoryCursorPageUseCase = mock(ViewSchoolDirectoryCursorPageUseCase.class);
        viewSchoolDirectoryPageUseCase = mock(ViewSchoolDirectoryPageUseCase.class);
        viewSchoolDirectoryDetailsUseCase = mock(ViewSchoolDirectoryDetailsUseCase.class);
        listSchoolsWithOnGoingAssessmentUseCase = mock(ListSchoolsWithOnGoingAssessmentUseCase.class);

        controller = new SchoolController(
            viewSchoolsUseCase,
            viewSchoolClassesUseCase,
            viewSchoolClassesByUserUseCase,
            viewMySchoolClassesUseCase,
            viewSchoolClassDetailsUseCase,
            viewSchoolClassUsersUseCase,
            updateSchoolClassUseCase,
            viewSchoolUsersBySchoolUseCase,
            viewSchoolUsersForRequesterUseCase,
            viewSchoolStudentsBySchoolUseCase,
            viewSchoolTeachersBySchoolUseCase,
            viewSchoolUserDetailsUseCase,
            updateSchoolUserUseCase, 
            updateSchoolUseCase, 
            viewSchoolRoomDetailsUseCase, 
            viewSchoolRoomsUseCase, 
            updateSchoolRoomUseCase, 
            updateSchoolGradeUseCase, 
            viewSchoolGradesUseCase,
            viewSchoolGradeDetailsUseCase,
            viewGradeLevelsUseCase,
            viewGradeLevelDetailsUseCase,
            updateGradeLevelUseCase,
            viewSchoolDirectoryCursorPageUseCase,
            viewSchoolDirectoryPageUseCase, 
            viewSchoolDirectoryDetailsUseCase,
            listSchoolsWithOnGoingAssessmentUseCase
        );
    }

    @Test
    void school_classes_should_return_page_result() {
        UUID languageId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        PageResult<SchoolClassDto> expected = new PageResult<SchoolClassDto>(List.of(), 1, 20, 0, 0);
        ViewSchoolClassesQuery query = new ViewSchoolClassesQuery(1, 20, "eng", "ACTIVE", languageId, gradeId);
        when(viewSchoolClassesUseCase.execute(query)).thenReturn(expected);

        PageResult<SchoolClassDto> result = controller.schoolClasses(1, 20, "eng", "ACTIVE", languageId, gradeId);

        assertThat(result).isEqualTo(expected);
        verify(viewSchoolClassesUseCase).execute(query);
    }

    @Test
    void school_classes_should_throw_when_page_or_size_invalid() {
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClasses(0, 20, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClasses(1, 0, null, null, null, null));
    }

    @Test
    void school_class_should_return_details() {
        UUID classId = UUID.randomUUID();
        SchoolClassDto expected = new SchoolClassDto(
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

        SchoolClassDto result = controller.schoolClass(classId);

        assertThat(result).isEqualTo(expected);
        verify(viewSchoolClassDetailsUseCase).execute(new ViewSchoolClassDetailsQuery(classId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_school_field_should_load_related_school() {
        UUID schoolId = UUID.randomUUID();
        SchoolClassDto response = schoolClassDto(schoolId, UUID.randomUUID(), UUID.randomUUID());
        SchoolDto expected = new SchoolDto(schoolId, "SCH", "School", null, null, null, null, null, 0, true, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader<UUID, SchoolDto> loader = mock(DataLoader.class);
        when(env.<UUID, SchoolDto>getDataLoader("schoolByClass")).thenReturn(loader);
        when(loader.load(schoolId)).thenReturn(CompletableFuture.completedFuture(expected));

        SchoolDto result = controller.school(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(schoolId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_school_grade_field_should_load_related_grade_with_school_scope() {
        UUID schoolId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        SchoolClassDto response = schoolClassDto(schoolId, UUID.randomUUID(), gradeId);
        SchoolGradeDto expected = new SchoolGradeDto(gradeId, schoolId, UUID.randomUUID(), "G10", "Grade 10", null, null, null, "ACTIVE", null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader<UUID, SchoolGradeDto> loader = mock(DataLoader.class);
        when(env.<UUID, SchoolGradeDto>getDataLoader("schoolGradeByClass")).thenReturn(loader);
        when(loader.load(gradeId)).thenReturn(CompletableFuture.completedFuture(expected));

        SchoolGradeDto result = controller.schoolGrade(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(gradeId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_language_field_should_load_related_language() {
        UUID languageId = UUID.randomUUID();
        SchoolClassDto response = schoolClassDto(UUID.randomUUID(), languageId, UUID.randomUUID());
        SupportedLanguageDto expected = new SupportedLanguageDto(languageId, "EN", "English", null, true, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader<UUID, SupportedLanguageDto> loader = mock(DataLoader.class);
        when(env.<UUID, SupportedLanguageDto>getDataLoader("supportedLanguageByClass")).thenReturn(loader);
        when(loader.load(languageId)).thenReturn(CompletableFuture.completedFuture(expected));

        SupportedLanguageDto result = controller.language(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(languageId);
    }

    @Test
    void school_class_users_should_return_page_result() {
        UUID classId = UUID.randomUUID();
        PageResult<SchoolClassUserDto> expected = new PageResult<SchoolClassUserDto>(List.of(), 1, 20, 0, 0);
        ViewSchoolClassUsersQuery query = new ViewSchoolClassUsersQuery(classId, 1, 20);
        when(viewSchoolClassUsersUseCase.execute(query)).thenReturn(expected);

        PageResult<SchoolClassUserDto> result = controller.schoolClassUsers(classId, 1, 20);

        assertThat(result).isEqualTo(expected);
        verify(viewSchoolClassUsersUseCase).execute(query);
    }

    @Test
    void school_class_users_should_throw_when_page_or_size_invalid() {
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 0, 20));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 1, 0));
    }

    @Test
    void school_classes_by_user_should_return_page_result() {
        PageResult<SchoolClassDto> expected = new PageResult<SchoolClassDto>(List.of(), 1, 20, 0, 0);
        ViewSchoolClassesByUserQuery query = new ViewSchoolClassesByUserQuery(schoolId, userId, null, 1, 20);
        when(viewSchoolClassesByUserUseCase.execute(query)).thenReturn(expected);

        PageResult<SchoolClassDto> result = controller.schoolClassesByUser(schoolId, userId, null, 1, 20);

        assertThat(result).isEqualTo(expected);
        verify(viewSchoolClassesByUserUseCase).execute(query);
    }

    @Test
    void school_classes_by_user_should_throw_when_page_or_size_invalid() {
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClassesByUser(schoolId, userId, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClassesByUser(schoolId, userId, null, 1, 0));
    }

    @Test
    void update_school_class_name_only_should_return_id_response() {
        UUID classId = UUID.randomUUID();
        Map<String, Object> input = Map.<String, Object>of("name", "English 02");
        UpdateSchoolClassCommand command = new UpdateSchoolClassCommand(classId, "English 02", true, null, false, null, false);
        UpdateSchoolClassResponse expected = new UpdateSchoolClassResponse(classId);
        when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

        UpdateSchoolClassResponse result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSchoolClassUseCase).execute(command);
    }

    @Test
    void update_school_class_description_null_should_map_presence() {
        UUID classId = UUID.randomUUID();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("description", null);
        UpdateSchoolClassCommand command = new UpdateSchoolClassCommand(classId, null, false, null, true, null, false);
        UpdateSchoolClassResponse expected = new UpdateSchoolClassResponse(classId);
        when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

        UpdateSchoolClassResponse result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSchoolClassUseCase).execute(command);
    }

    @Test
    void update_school_class_status_only_should_map_presence() {
        UUID classId = UUID.randomUUID();
        Map<String, Object> input = Map.<String, Object>of("status", "INACTIVE");
        UpdateSchoolClassCommand command = new UpdateSchoolClassCommand(classId, null, false, null, false, "INACTIVE", true);
        UpdateSchoolClassResponse expected = new UpdateSchoolClassResponse(classId);
        when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

        UpdateSchoolClassResponse result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateSchoolClassUseCase).execute(command);
    }

    @Test
    void school_users_field_should_return_page_from_use_case() {
        SchoolUserDto response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        PageResult<SchoolUserDto> page = new PageResult<>(List.of(response), 1, 20, 1, 1);
        when(viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null))).thenReturn(page);

        PageResult<SchoolUserDto> result = controller.schoolUsersBySchool(schoolId, 1, 20, null, null, null, null, null);

        assertThat(result).isEqualTo(page);
        assertThat(result.content()).containsExactly(response);
        verify(viewSchoolUsersBySchoolUseCase).execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null));
    }

    @Test
    void school_students_by_school_should_return_page_from_use_case() {
        SchoolUserDto response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        PageResult<SchoolUserDto> page = new PageResult<>(List.of(response), 1, 20, 1, 1);
        when(viewSchoolStudentsBySchoolUseCase.execute(new ViewSchoolStudentsBySchoolQuery(schoolId, 1, 20, null, null))).thenReturn(page);

        PageResult<SchoolUserDto> result = controller.schoolStudentsBySchool(schoolId, 1, 20, null, null);

        assertThat(result).isEqualTo(page);
        verify(viewSchoolStudentsBySchoolUseCase).execute(new ViewSchoolStudentsBySchoolQuery(schoolId, 1, 20, null, null));
    }

    @Test
    void school_students_by_school_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.schoolStudentsBySchool(schoolId, 0, 20, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Số trang yêu cầu không hợp lệ");
    }

    @Test
    void school_teachers_by_school_should_return_page_from_use_case() {
        SchoolUserDto response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        PageResult<SchoolUserDto> page = new PageResult<>(List.of(response), 1, 20, 1, 1);
        when(viewSchoolTeachersBySchoolUseCase.execute(new ViewSchoolTeachersBySchoolQuery(schoolId, 1, 20, null, null))).thenReturn(page);

        PageResult<SchoolUserDto> result = controller.schoolTeachersBySchool(schoolId, 1, 20, null, null);

        assertThat(result).isEqualTo(page);
        verify(viewSchoolTeachersBySchoolUseCase).execute(new ViewSchoolTeachersBySchoolQuery(schoolId, 1, 20, null, null));
    }

    @Test
    void school_teachers_by_school_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.schoolTeachersBySchool(schoolId, 0, 20, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Số trang yêu cầu không hợp lệ");
    }

    @Test
    void school_users_by_school_field_should_return_details_from_use_case() {
        SchoolUserDto response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        when(viewSchoolUserDetailsUseCase.execute(new ViewSchoolUserDetailsQuery(schoolId, userId))).thenReturn(response);

        SchoolUserDto result = controller.schoolUser(schoolId, userId);

        assertThat(result).isEqualTo(response);
        verify(viewSchoolUserDetailsUseCase).execute(new ViewSchoolUserDetailsQuery(schoolId, userId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_user_user_field_should_load_related_user() {
        SchoolUserDto response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        UserDto expected = new UserDto(userId, "student@example.com", null, "Student", null, null, null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader<UUID, UserDto> loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userBySchoolUser")).thenReturn(loader);
        when(loader.load(userId)).thenReturn(CompletableFuture.completedFuture(expected));

        UserDto result = controller.schoolUserUser(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(userId);
    }

    @Test
    void school_users_by_school_field_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.schoolUsersBySchool(schoolId, 0, 20, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Số trang yêu cầu không hợp lệ");
    }

    private SchoolUserDto schoolUserDto(UUID id, UUID schoolId, UUID userId) {
        return new SchoolUserDto(id, schoolId, userId, null, null);
    }

    private static SchoolClassDto schoolClassDto(UUID schoolId, UUID languageId, UUID gradeId) {
        return new SchoolClassDto(
            UUID.randomUUID(),
            schoolId,
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class",
            "ACTIVE",
            "2026-06-06T12:00:00Z",
            "2026-06-06T12:00:00Z"
        );
    }

    @Test
    void schools_should_reject_page_zero_missing_page_and_a_size_above_the_cap() {
        assertThrows(IllegalArgumentException.class, () -> controller.schools(0, 20, null, null));
        assertThrows(IllegalArgumentException.class, () -> controller.schools(null, 20, null, null));
        assertThrows(IllegalArgumentException.class, () -> controller.schools(1, 201, null, null));

        verifyNoInteractions(viewSchoolsUseCase);
    }

    @Test
    void school_rooms_should_reject_page_zero_and_missing_paging_instead_of_clamping() {
        assertThrows(IllegalArgumentException.class, () -> controller.schoolRooms(schoolId, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolRooms(schoolId, null, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolRooms(schoolId, 1, null));

        verifyNoInteractions(viewSchoolRoomsUseCase);
    }

    @Test
    void school_grades_should_reject_page_zero_and_missing_paging_instead_of_clamping() {
        assertThrows(IllegalArgumentException.class, () -> controller.schoolGrades(schoolId, null, null, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolGrades(schoolId, null, null, null, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolGrades(schoolId, null, null, 1, null));

        verifyNoInteractions(viewSchoolGradesUseCase);
    }
}
