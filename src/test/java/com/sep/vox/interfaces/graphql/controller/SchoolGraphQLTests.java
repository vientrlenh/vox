package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.schoolclass.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.command.gradelevel.UpdateGradeLevelCommand;
import com.sep.vox.application.port.input.command.school.UpdateSchoolCommand;
import com.sep.vox.application.port.input.command.schoolgrade.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.command.schoolroom.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;
import com.sep.vox.application.port.input.query.gradelevel.ViewGradeLevelsQuery;
import com.sep.vox.application.port.input.query.schoolclass.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.schoolgrade.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.gradelevel.UpdateGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelDetailsUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelsUseCase;
import com.sep.vox.application.port.input.usecase.school.ListSchoolsWithOnGoingAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMySchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesByUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryCursorPageUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryPageUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
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
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolDirectoryDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends school operations through the real schema and asserts the typed values: the {@code DateTime} and
 * {@code Date} scalars and the status and origin enums, on output and on input.
 */
@GraphQLControllerTest(GraphQLSchoolController.class)
@MockitoBean(types = {
    ViewSchoolClassesByUserUseCase.class, ViewMySchoolClassesUseCase.class, ViewSchoolClassDetailsUseCase.class,
    ViewSchoolStudentsBySchoolUseCase.class, ViewSchoolTeachersBySchoolUseCase.class,
    ViewSchoolUsersForRequesterUseCase.class, ViewSchoolUserDetailsUseCase.class,
    ViewSchoolRoomsUseCase.class,
    ViewSchoolGradeDetailsUseCase.class, ViewGradeLevelDetailsUseCase.class,
    ViewSchoolDirectoryCursorPageUseCase.class, ViewSchoolDirectoryDetailsUseCase.class,
    ListSchoolsWithOnGoingAssessmentUseCase.class })
class SchoolGraphQLTests {

    private static final UUID ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSchoolsUseCase viewSchoolsUseCase;
    @MockitoBean
    private ViewSchoolClassesUseCase viewSchoolClassesUseCase;
    @MockitoBean
    private ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase;
    @MockitoBean
    private UpdateSchoolClassUseCase updateSchoolClassUseCase;
    @MockitoBean
    private UpdateSchoolUserUseCase updateSchoolUserUseCase;
    @MockitoBean
    private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    @MockitoBean
    private ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    @MockitoBean
    private ViewSchoolGradesUseCase viewSchoolGradesUseCase;
    @MockitoBean
    private UpdateSchoolGradeUseCase updateSchoolGradeUseCase;
    @MockitoBean
    private UpdateSchoolUseCase updateSchoolUseCase;
    @MockitoBean
    private UpdateSchoolRoomUseCase updateSchoolRoomUseCase;
    @MockitoBean
    private UpdateGradeLevelUseCase updateGradeLevelUseCase;
    @MockitoBean
    private ViewGradeLevelsUseCase viewGradeLevelsUseCase;
    @MockitoBean
    private ViewSchoolDirectoryPageUseCase viewSchoolDirectoryPageUseCase;

    private static <T> PageResult<T> pageOf(T element) {
        return new PageResult<>(List.of(element), 1, 20, 1L, 1);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void schools_should_return_created_and_updated_at_as_date_time() {
        SchoolDto school = new SchoolDto(ID, "VOX", "Vox", null, null, null, null, null, 0, true, CREATED_AT, null);
        when(viewSchoolsUseCase.execute(any())).thenReturn(pageOf(school));

        graphQlTester.document("{ schools { content { createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("schools.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schools.content[0].updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_classes_should_return_status_enum_and_date_time_values() {
        SchoolClassDto schoolClass = new SchoolClassDto(ID, ID, ID, ID, "ENG-01", "English 01", null,
            SchoolClassStatus.ACTIVE, CREATED_AT, null);
        when(viewSchoolClassesUseCase.execute(any())).thenReturn(pageOf(schoolClass));

        graphQlTester.document("{ schoolClasses { content { status createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("schoolClasses.content[0].status").entity(String.class).isEqualTo("ACTIVE")
            .path("schoolClasses.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schoolClasses.content[0].updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_classes_should_bind_the_status_variable_to_the_enum() {
        when(viewSchoolClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 20, 0L, 0));

        graphQlTester.document("query($status: SchoolClassStatus) { schoolClasses(status: $status) { totalElements } }")
            .variable("status", "ARCHIVED").execute()
            .errors().verify();

        verify(viewSchoolClassesUseCase).execute(new ViewSchoolClassesQuery(1, 20, null, SchoolClassStatus.ARCHIVED, null, null));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_classes_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ schoolClasses(status: NOT_A_STATUS) { totalElements } }").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewSchoolClassesUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_class_should_deliver_the_status_variable_as_the_enum() {
        when(updateSchoolClassUseCase.execute(any())).thenReturn(new UpdateSchoolClassResponse(ID));

        graphQlTester.document("mutation($status: SchoolClassStatus) { updateSchoolClass(id: \"%s\", input: { status: $status }) { schoolClassId } }".formatted(ID))
            .variable("status", "INACTIVE").execute()
            .errors().verify();

        verify(updateSchoolClassUseCase).execute(
            new UpdateSchoolClassCommand(ID, null, false, null, false, SchoolClassStatus.INACTIVE, true));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_class_should_tell_an_explicit_null_description_from_an_absent_one() {
        when(updateSchoolClassUseCase.execute(any())).thenReturn(new UpdateSchoolClassResponse(ID));

        graphQlTester.document("mutation { updateSchoolClass(id: \"%s\", input: { description: null }) { schoolClassId } }".formatted(ID))
            .execute().errors().verify();
        graphQlTester.document("mutation { updateSchoolClass(id: \"%s\", input: { name: \"English 02\" }) { schoolClassId } }".formatted(ID))
            .execute().errors().verify();

        verify(updateSchoolClassUseCase).execute(
            new UpdateSchoolClassCommand(ID, null, false, null, true, null, false));
        verify(updateSchoolClassUseCase).execute(
            new UpdateSchoolClassCommand(ID, "English 02", true, null, false, null, false));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_user_should_tell_an_explicit_null_avatar_from_an_absent_one() {
        when(updateSchoolUserUseCase.execute(any())).thenReturn(new UpdateSchoolUserResponse(ID));

        executeUpdateSchoolUser("{ avatarUrl: null }");
        executeUpdateSchoolUser("{ phone: \"0911000001\" }");

        verify(updateSchoolUserUseCase).execute(new UpdateSchoolUserCommand(
            ID, ID, null, false, null, false, null, false, null, false, null, true));
        verify(updateSchoolUserUseCase).execute(new UpdateSchoolUserCommand(
            ID, ID, null, false, "0911000001", true, null, false, null, false, null, false));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_user_should_deliver_the_date_of_birth_as_a_local_date() {
        when(updateSchoolUserUseCase.execute(any())).thenReturn(new UpdateSchoolUserResponse(ID));

        executeUpdateSchoolUser("{ dateOfBirth: \"2008-05-01\" }");

        verify(updateSchoolUserUseCase).execute(new UpdateSchoolUserCommand(
            ID, ID, null, false, null, false, null, false, LocalDate.of(2008, 5, 1), true, null, false));
    }

    /**
     * The payload is not selected: {@code UpdateSchoolUserResponse} exposes {@code id} while the schema type
     * declares {@code schoolUserId}, so selecting it would fail on an unrelated mismatch.
     */
    private void executeUpdateSchoolUser(String input) {
        graphQlTester.document("mutation { updateSchoolUser(schoolId: \"%s\", userId: \"%s\", input: %s) { __typename } }"
            .formatted(ID, ID, input)).execute().errors().verify();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_class_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("mutation { updateSchoolClass(id: \"%s\", input: { status: DELETED }) { schoolClassId } }".formatted(ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("DELETED"))
            .verify();

        verifyNoInteractions(updateSchoolClassUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_class_users_should_return_joined_and_left_at_as_date_time() {
        SchoolClassUserDto member = new SchoolClassUserDto(ID, ID, ID, false,
            Instant.parse("2026-01-05T01:00:00Z"), Instant.parse("2026-06-01T10:00:00Z"), null);
        when(viewSchoolClassUsersUseCase.execute(any())).thenReturn(pageOf(member));

        graphQlTester.document("{ schoolClassUsers(schoolClassId: \"%s\") { content { joinedAt leftAt } } }".formatted(ID))
            .execute()
            .errors().verify()
            .path("schoolClassUsers.content[0].joinedAt").entity(String.class).isEqualTo("2026-01-05T01:00:00Z")
            .path("schoolClassUsers.content[0].leftAt").entity(String.class).isEqualTo("2026-06-01T10:00:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_users_by_school_should_return_dates_and_bind_the_user_status_variable() {
        SchoolUserDto schoolUser = new SchoolUserDto(ID, ID, ID, CREATED_AT, null);
        when(viewSchoolUsersBySchoolUseCase.execute(any())).thenReturn(pageOf(schoolUser));

        graphQlTester.document("query($status: UserStatus) { schoolUsersBySchool(schoolId: \"%s\", status: $status) { content { startDate endDate } } }".formatted(ID))
            .variable("status", "LOCKED").execute()
            .errors().verify()
            .path("schoolUsersBySchool.content[0].startDate").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schoolUsersBySchool.content[0].endDate").valueIsNull();

        verify(viewSchoolUsersBySchoolUseCase).execute(
            new ViewSchoolUsersBySchoolQuery(ID, 1, 20, null, null, null, UserStatus.LOCKED, null));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_users_by_school_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ schoolUsersBySchool(schoolId: \"%s\", status: NOT_A_STATUS) { totalElements } }".formatted(ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewSchoolUsersBySchoolUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_room_should_return_created_and_updated_at_as_date_time() {
        SchoolRoomFromDto room = new SchoolRoomFromDto(ID, ID, "R1", "Room 1", null, true, CREATED_AT, null, null, null);
        when(viewSchoolRoomDetailsUseCase.execute(any())).thenReturn(room);

        graphQlTester.document("{ schoolRoom(id: \"%s\") { createdAt updatedAt } }".formatted(ID)).execute()
            .errors().verify()
            .path("schoolRoom.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schoolRoom.updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_grades_should_return_dates_and_status_enum_and_bind_the_status_variable() {
        SchoolGradeDto grade = new SchoolGradeDto(ID, ID, ID, "2026", "School year 2026", null,
            LocalDate.of(2026, 9, 5), LocalDate.of(2027, 5, 31), SchoolGradeStatus.ARCHIVED, CREATED_AT, null);
        when(viewSchoolGradesUseCase.execute(any())).thenReturn(pageOf(grade));

        graphQlTester.document("query($status: SchoolGradeStatus) { schoolGrades(schoolId: \"%s\", status: $status, page: 1, size: 20) { content { startDate endDate status createdAt } } }".formatted(ID))
            .variable("status", "ARCHIVED").execute()
            .errors().verify()
            .path("schoolGrades.content[0].startDate").entity(String.class).isEqualTo("2026-09-05")
            .path("schoolGrades.content[0].endDate").entity(String.class).isEqualTo("2027-05-31")
            .path("schoolGrades.content[0].status").entity(String.class).isEqualTo("ARCHIVED")
            .path("schoolGrades.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");

        verify(viewSchoolGradesUseCase).execute(new ViewSchoolGradesQuery(ID, null, SchoolGradeStatus.ARCHIVED, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_grade_should_deliver_date_variables_as_local_dates() {
        when(updateSchoolGradeUseCase.execute(any())).thenReturn(ID);

        graphQlTester.document("mutation($startDate: Date) { updateSchoolGrade(id: \"%s\", input: { startDate: $startDate }) }".formatted(ID))
            .variable("startDate", "2026-09-05").execute()
            .errors().verify();

        ArgumentCaptor<UpdateSchoolGradeCommand> captor = ArgumentCaptor.forClass(UpdateSchoolGradeCommand.class);
        verify(updateSchoolGradeUseCase).execute(captor.capture());
        assertThat(captor.getValue().startDate()).isEqualTo(LocalDate.of(2026, 9, 5));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_grade_should_reject_a_start_date_that_is_not_yyyy_mm_dd() {
        graphQlTester.document("mutation($startDate: Date) { updateSchoolGrade(id: \"%s\", input: { startDate: $startDate }) }".formatted(ID))
            .variable("startDate", "05/09/2026").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("startDate"))
            .verify();

        verifyNoInteractions(updateSchoolGradeUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void grade_levels_should_return_status_enum_and_date_time_values() {
        GradeLevelDto level = new GradeLevelDto(ID, "G10", "Grade 10", null, 10, GradeLevelStatus.INACTIVE, CREATED_AT, null);
        when(viewGradeLevelsUseCase.execute(any())).thenReturn(pageOf(level));

        graphQlTester.document("query($status: GradeLevelStatus) { gradeLevels(status: $status) { content { status createdAt updatedAt } } }")
            .variable("status", "INACTIVE").execute()
            .errors().verify()
            .path("gradeLevels.content[0].status").entity(String.class).isEqualTo("INACTIVE")
            .path("gradeLevels.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("gradeLevels.content[0].updatedAt").valueIsNull();

        verify(viewGradeLevelsUseCase).execute(new ViewGradeLevelsQuery(1, 20, null, GradeLevelStatus.INACTIVE));
    }

    @Test
    @WithAnonymousUser
    void school_directory_page_should_return_origin_enum_and_date_time_values() {
        SchoolDirectoryDto directory = new SchoolDirectoryDto(ID, "001", "Vox High School", null, null, null, null, null,
            SchoolDirectoryOrigin.ADMIN_CREATED, true, CREATED_AT, CREATED_AT);
        when(viewSchoolDirectoryPageUseCase.execute(any())).thenReturn(pageOf(directory));

        graphQlTester.document("{ schoolDirectoryPage { content { origin createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("schoolDirectoryPage.content[0].origin").entity(String.class).isEqualTo("ADMIN_CREATED")
            .path("schoolDirectoryPage.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schoolDirectoryPage.content[0].updatedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_should_distinguish_omitted_null_and_value_for_description_and_domain() {
        when(updateSchoolUseCase.execute(any())).thenReturn(ID);
        String mutation = "mutation { updateSchool(id: \"%s\", input: { name: \"N\" %s }) }";

        graphQlTester.document(mutation.formatted(ID, "")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: null, domain: null")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: \"Text\", domain: \"vox.edu.vn\"")).execute().errors().verify();

        verify(updateSchoolUseCase).execute(new UpdateSchoolCommand(ID, "N", null, false, null, null, null, false, null, null));
        verify(updateSchoolUseCase).execute(new UpdateSchoolCommand(ID, "N", null, true, null, null, null, true, null, null));
        verify(updateSchoolUseCase).execute(new UpdateSchoolCommand(ID, "N", "Text", true, null, null, "vox.edu.vn", true, null, null));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_room_should_distinguish_omitted_null_and_value_description() {
        when(updateSchoolRoomUseCase.execute(any())).thenReturn(ID);
        String mutation = "mutation { updateSchoolRoom(id: \"%s\", input: { name: \"N\" %s }) }";

        graphQlTester.document(mutation.formatted(ID, "")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: null")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: \"Text\"")).execute().errors().verify();

        verify(updateSchoolRoomUseCase).execute(new UpdateSchoolRoomCommand(ID, "N", null, false));
        verify(updateSchoolRoomUseCase).execute(new UpdateSchoolRoomCommand(ID, "N", null, true));
        verify(updateSchoolRoomUseCase).execute(new UpdateSchoolRoomCommand(ID, "N", "Text", true));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_grade_should_distinguish_omitted_null_and_value_description() {
        when(updateSchoolGradeUseCase.execute(any())).thenReturn(ID);
        String mutation = "mutation { updateSchoolGrade(id: \"%s\", input: { name: \"N\" %s }) }";

        graphQlTester.document(mutation.formatted(ID, "")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: null")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: \"Text\"")).execute().errors().verify();

        verify(updateSchoolGradeUseCase).execute(new UpdateSchoolGradeCommand(ID, "N", null, false, null, null));
        verify(updateSchoolGradeUseCase).execute(new UpdateSchoolGradeCommand(ID, "N", null, true, null, null));
        verify(updateSchoolGradeUseCase).execute(new UpdateSchoolGradeCommand(ID, "N", "Text", true, null, null));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_grade_level_should_distinguish_omitted_null_and_value_description() {
        when(updateGradeLevelUseCase.execute(any())).thenReturn(ID);
        String mutation = "mutation { updateGradeLevel(gradeLevelId: \"%s\", input: { name: \"N\" %s }) }";

        graphQlTester.document(mutation.formatted(ID, "")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: null")).execute().errors().verify();
        graphQlTester.document(mutation.formatted(ID, ", description: \"Text\"")).execute().errors().verify();

        verify(updateGradeLevelUseCase).execute(new UpdateGradeLevelCommand(ID, "N", null, false, null));
        verify(updateGradeLevelUseCase).execute(new UpdateGradeLevelCommand(ID, "N", null, true, null));
        verify(updateGradeLevelUseCase).execute(new UpdateGradeLevelCommand(ID, "N", "Text", true, null));
    }
}
