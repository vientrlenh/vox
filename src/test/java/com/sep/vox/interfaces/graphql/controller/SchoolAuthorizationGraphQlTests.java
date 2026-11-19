package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.gradelevel.UpdateGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelDetailsUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelsUseCase;
import com.sep.vox.application.port.input.usecase.school.ListSchoolsWithOnGoingExamUseCase;
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
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs a representative set of {@link SchoolController} operations through the real schema and
 * {@code @PreAuthorize}, to prove the role checks are enforced: one operation per distinct role rule
 * (system admin only, school admin only, school admin or teacher), plus the unprotected school directory.
 * The direct-call behaviour of every operation stays covered by {@link SchoolControllerTests}.
 */
@GraphQlControllerTest(SchoolController.class)
@MockitoBean(types = {
    ViewSchoolClassesUseCase.class, ViewSchoolClassesByUserUseCase.class, ViewMySchoolClassesUseCase.class,
    ViewSchoolClassDetailsUseCase.class, ViewSchoolClassUsersUseCase.class, UpdateSchoolClassUseCase.class,
    ViewSchoolStudentsBySchoolUseCase.class, ViewSchoolTeachersBySchoolUseCase.class,
    ViewSchoolUserDetailsUseCase.class, UpdateSchoolUserUseCase.class, ViewSchoolRoomDetailsUseCase.class,
    ViewSchoolRoomsUseCase.class, UpdateSchoolRoomUseCase.class, UpdateSchoolGradeUseCase.class,
    ViewSchoolGradesUseCase.class, ViewSchoolGradeDetailsUseCase.class, ViewGradeLevelsUseCase.class,
    ViewGradeLevelDetailsUseCase.class, ViewSchoolDirectoryCursorPageUseCase.class,
    ViewSchoolDirectoryDetailsUseCase.class })
class SchoolAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String SCHOOLS_QUERY = "{ schools { totalElements } }";
    private static final String SCHOOL_USERS_BY_SCHOOL_QUERY =
        "{ schoolUsersBySchool(schoolId: \"%s\") { totalElements } }".formatted(ID);
    private static final String SCHOOL_USERS_FOR_REQUESTER_QUERY =
        "{ schoolUsersForRequester(schoolId: \"%s\") { totalElements } }".formatted(ID);
    private static final String SCHOOLS_WITH_ONGOING_EXAM_QUERY = "{ schoolsWithOngoingExam }";
    private static final String SCHOOL_DIRECTORY_PAGE_QUERY = "{ schoolDirectoryPage { totalElements } }";
    private static final String UPDATE_SCHOOL_MUTATION =
        "mutation { updateSchool(id: \"%s\", input: { name: \"Vox High School\" }) }".formatted(ID);
    private static final String UPDATE_GRADE_LEVEL_MUTATION =
        "mutation { updateGradeLevel(gradeLevelId: \"%s\", input: { name: \"Grade 10\" }) }".formatted(ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSchoolsUseCase viewSchoolsUseCase;
    @MockitoBean
    private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    @MockitoBean
    private ViewSchoolUsersForRequesterUseCase viewSchoolUsersForRequesterUseCase;
    @MockitoBean
    private UpdateSchoolUseCase updateSchoolUseCase;
    @MockitoBean
    private UpdateGradeLevelUseCase updateGradeLevelUseCase;
    @MockitoBean
    private ListSchoolsWithOnGoingExamUseCase listSchoolsWithOnGoingExamUseCase;
    @MockitoBean
    private ViewSchoolDirectoryPageUseCase viewSchoolDirectoryPageUseCase;

    private static <T> PageResult<T> emptyPage() {
        return new PageResult<>(List.of(), 1, 20, 0L, 0);
    }

    private void assertTotalElementsIsZero(String document, String operationName) {
        graphQlTester.document(document).execute()
            .errors().verify()
            .path(operationName + ".totalElements").entity(Integer.class).isEqualTo(0);
    }

    private void assertErrorType(String document, ErrorType expectedErrorType) {
        graphQlTester.document(document).execute()
            .errors().expect(error -> error.getErrorType() == expectedErrorType).verify();
    }

    private static Stream<String> protectedOperations() {
        return Stream.of(SCHOOLS_QUERY, SCHOOL_USERS_BY_SCHOOL_QUERY, SCHOOL_USERS_FOR_REQUESTER_QUERY,
            SCHOOLS_WITH_ONGOING_EXAM_QUERY, UPDATE_SCHOOL_MUTATION, UPDATE_GRADE_LEVEL_MUTATION);
    }

    @ParameterizedTest
    @MethodSource("protectedOperations")
    @WithAnonymousUser
    void every_protected_operation_should_be_unauthorized_without_authentication(String document) {
        assertErrorType(document, ErrorType.UNAUTHORIZED);

        verifyNoInteractions(viewSchoolsUseCase, viewSchoolUsersBySchoolUseCase,
            viewSchoolUsersForRequesterUseCase, updateSchoolUseCase, updateGradeLevelUseCase,
            listSchoolsWithOnGoingExamUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void schools_should_return_data_for_system_admin() {
        when(viewSchoolsUseCase.execute(any())).thenReturn(emptyPage());

        assertTotalElementsIsZero(SCHOOLS_QUERY, "schools");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void schools_should_be_forbidden_for_school_admin() {
        assertErrorType(SCHOOLS_QUERY, ErrorType.FORBIDDEN);

        verifyNoInteractions(viewSchoolsUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_users_by_school_should_return_data_for_school_admin() {
        when(viewSchoolUsersBySchoolUseCase.execute(any())).thenReturn(emptyPage());

        assertTotalElementsIsZero(SCHOOL_USERS_BY_SCHOOL_QUERY, "schoolUsersBySchool");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void school_users_by_school_should_be_forbidden_for_teacher() {
        assertErrorType(SCHOOL_USERS_BY_SCHOOL_QUERY, ErrorType.FORBIDDEN);

        verifyNoInteractions(viewSchoolUsersBySchoolUseCase);
    }

    private void assertSchoolUsersForRequesterReturnsData() {
        when(viewSchoolUsersForRequesterUseCase.execute(any())).thenReturn(emptyPage());

        assertTotalElementsIsZero(SCHOOL_USERS_FOR_REQUESTER_QUERY, "schoolUsersForRequester");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_users_for_requester_should_return_data_for_school_admin() {
        assertSchoolUsersForRequesterReturnsData();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void school_users_for_requester_should_return_data_for_teacher() {
        assertSchoolUsersForRequesterReturnsData();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void school_users_for_requester_should_be_forbidden_for_student() {
        assertErrorType(SCHOOL_USERS_FOR_REQUESTER_QUERY, ErrorType.FORBIDDEN);

        verifyNoInteractions(viewSchoolUsersForRequesterUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_school_should_return_school_id_for_school_admin() {
        when(updateSchoolUseCase.execute(any())).thenReturn(ID);

        graphQlTester.document(UPDATE_SCHOOL_MUTATION).execute()
            .errors().verify()
            .path("updateSchool").entity(String.class).isEqualTo(ID.toString());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_school_should_be_forbidden_for_system_admin() {
        assertErrorType(UPDATE_SCHOOL_MUTATION, ErrorType.FORBIDDEN);

        verifyNoInteractions(updateSchoolUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_grade_level_should_return_grade_level_id_for_system_admin() {
        when(updateGradeLevelUseCase.execute(any())).thenReturn(ID);

        graphQlTester.document(UPDATE_GRADE_LEVEL_MUTATION).execute()
            .errors().verify()
            .path("updateGradeLevel").entity(String.class).isEqualTo(ID.toString());
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_grade_level_should_be_forbidden_for_school_admin() {
        assertErrorType(UPDATE_GRADE_LEVEL_MUTATION, ErrorType.FORBIDDEN);

        verifyNoInteractions(updateGradeLevelUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void schools_with_ongoing_exam_should_return_ids_for_system_admin() {
        when(listSchoolsWithOnGoingExamUseCase.execute(any())).thenReturn(List.of(ID));

        graphQlTester.document(SCHOOLS_WITH_ONGOING_EXAM_QUERY).execute()
            .errors().verify()
            .path("schoolsWithOngoingExam").entityList(String.class).containsExactly(ID.toString());
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void schools_with_ongoing_exam_should_be_forbidden_for_school_admin() {
        assertErrorType(SCHOOLS_WITH_ONGOING_EXAM_QUERY, ErrorType.FORBIDDEN);

        verifyNoInteractions(listSchoolsWithOnGoingExamUseCase);
    }

    @Test
    @WithAnonymousUser
    void school_directory_page_should_be_readable_without_authentication() {
        when(viewSchoolDirectoryPageUseCase.execute(any())).thenReturn(emptyPage());

        assertTotalElementsIsZero(SCHOOL_DIRECTORY_PAGE_QUERY, "schoolDirectoryPage");
    }
}
