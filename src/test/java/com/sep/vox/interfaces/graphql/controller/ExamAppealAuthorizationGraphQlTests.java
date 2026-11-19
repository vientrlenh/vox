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

import com.sep.vox.application.port.input.usecase.examappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsByExamUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealsUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs every {@link ExamAppealController} query through the real schema and {@code @PreAuthorize}, to prove
 * the three role groups are enforced: school admin only, school admin or teacher, and student only.
 * The direct-call behaviour stays covered by {@link ExamAppealControllerTests}.
 */
@GraphQlControllerTest(ExamAppealController.class)
class ExamAppealAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String APPEALS_QUERY = "{ appeals { totalElements } }";
    private static final String APPEAL_STATS_QUERY = "{ appealStats { pending } }";
    private static final String EXAM_APPEALS_QUERY = "{ examAppeals(examId: \"%s\") { totalElements } }".formatted(ID);
    private static final String APPEAL_QUERY = "{ appeal(id: \"%s\") { id } }".formatted(ID);
    private static final String APPEAL_REVIEWERS_QUERY = "{ appealReviewers(appealId: \"%s\") { id } }".formatted(ID);
    private static final String MY_APPEALS_QUERY = "{ myAppeals { totalElements } }";
    private static final String MY_APPEAL_QUERY = "{ myAppeal(id: \"%s\") { id } }".formatted(ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewExamAppealsUseCase viewExamAppealsUseCase;
    @MockitoBean
    private ViewExamAppealStatsUseCase viewExamAppealStatsUseCase;
    @MockitoBean
    private ViewExamAppealDetailUseCase viewExamAppealDetailUseCase;
    @MockitoBean
    private ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    @MockitoBean
    private ViewMyAppealsUseCase viewMyAppealsUseCase;
    @MockitoBean
    private ViewMyAppealDetailUseCase viewMyAppealDetailUseCase;
    @MockitoBean
    private ViewExamAppealsByExamUseCase viewExamAppealsByExamUseCase;

    private static <T> PageResult<T> emptyPage() {
        return new PageResult<>(List.of(), 1, 20, 0L, 0);
    }

    private static AppealDetailInfo appealDetailWithOnlyId() {
        return new AppealDetailInfo(ID, null, null, null, null, "PENDING", null, null, null, null, null, null,
            null, null, null, null, List.of(), null, false, null, null);
    }

    private void assertTotalElementsIsZero(String document, String operationName) {
        graphQlTester.document(document).execute()
            .errors().verify()
            .path(operationName + ".totalElements").entity(Integer.class).isEqualTo(0);
    }

    private void assertIdIsReturned(String document, String operationName) {
        graphQlTester.document(document).execute()
            .errors().verify()
            .path(operationName + ".id").entity(String.class).isEqualTo(ID.toString());
    }

    private void assertErrorType(String document, ErrorType expectedErrorType) {
        graphQlTester.document(document).execute()
            .errors().expect(error -> error.getErrorType() == expectedErrorType).verify();
    }

    private void verifyNoUseCaseWasCalled() {
        verifyNoInteractions(viewExamAppealsUseCase, viewExamAppealStatsUseCase, viewExamAppealDetailUseCase,
            viewAssignableReviewersUseCase, viewMyAppealsUseCase, viewMyAppealDetailUseCase,
            viewExamAppealsByExamUseCase);
    }

    private static Stream<String> everyQuery() {
        return Stream.of(APPEALS_QUERY, APPEAL_STATS_QUERY, EXAM_APPEALS_QUERY, APPEAL_QUERY,
            APPEAL_REVIEWERS_QUERY, MY_APPEALS_QUERY, MY_APPEAL_QUERY);
    }

    private static Stream<String> schoolAdminOrTeacherQueries() {
        return Stream.of(EXAM_APPEALS_QUERY, APPEAL_QUERY, APPEAL_REVIEWERS_QUERY);
    }

    private static Stream<String> schoolAdminOnlyQueries() {
        return Stream.of(APPEALS_QUERY, APPEAL_STATS_QUERY);
    }

    private static Stream<String> studentOnlyQueries() {
        return Stream.of(MY_APPEALS_QUERY, MY_APPEAL_QUERY);
    }

    @ParameterizedTest
    @MethodSource("everyQuery")
    @WithAnonymousUser
    void every_query_should_be_unauthorized_without_authentication(String document) {
        assertErrorType(document, ErrorType.UNAUTHORIZED);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("schoolAdminOnlyQueries")
    @WithMockUser(roles = "TEACHER")
    void school_admin_only_queries_should_be_forbidden_for_teacher(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("schoolAdminOrTeacherQueries")
    @WithMockUser(roles = "STUDENT")
    void school_admin_or_teacher_queries_should_be_forbidden_for_student(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("studentOnlyQueries")
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void student_only_queries_should_be_forbidden_for_school_admin(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("studentOnlyQueries")
    @WithMockUser(roles = "TEACHER")
    void student_only_queries_should_be_forbidden_for_teacher(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void appeals_should_return_data_for_school_admin() {
        when(viewExamAppealsUseCase.execute(any())).thenReturn(emptyPage());

        assertTotalElementsIsZero(APPEALS_QUERY, "appeals");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void appeal_stats_should_return_data_for_school_admin() {
        when(viewExamAppealStatsUseCase.execute()).thenReturn(new AppealStatsInfo(3, 0, 0, 0, 0));

        graphQlTester.document(APPEAL_STATS_QUERY).execute()
            .errors().verify()
            .path("appealStats.pending").entity(Integer.class).isEqualTo(3);
    }

    private void assertAppealReadableAsSchoolAdminOrTeacher() {
        when(viewExamAppealsByExamUseCase.execute(any())).thenReturn(emptyPage());
        when(viewExamAppealDetailUseCase.execute(any())).thenReturn(appealDetailWithOnlyId());
        when(viewAssignableReviewersUseCase.execute(any()))
            .thenReturn(List.of(new AppealReviewerLiteInfo(ID, "Reviewer", 0L, false)));

        assertTotalElementsIsZero(EXAM_APPEALS_QUERY, "examAppeals");
        assertIdIsReturned(APPEAL_QUERY, "appeal");
        graphQlTester.document(APPEAL_REVIEWERS_QUERY).execute()
            .errors().verify()
            .path("appealReviewers[0].id").entity(String.class).isEqualTo(ID.toString());
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void exam_appeals_detail_and_reviewers_should_return_data_for_school_admin() {
        assertAppealReadableAsSchoolAdminOrTeacher();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void exam_appeals_detail_and_reviewers_should_return_data_for_teacher() {
        assertAppealReadableAsSchoolAdminOrTeacher();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_appeals_and_my_appeal_should_return_data_for_student() {
        when(viewMyAppealsUseCase.execute(any())).thenReturn(emptyPage());
        when(viewMyAppealDetailUseCase.execute(any())).thenReturn(appealDetailWithOnlyId());

        assertTotalElementsIsZero(MY_APPEALS_QUERY, "myAppeals");
        assertIdIsReturned(MY_APPEAL_QUERY, "myAppeal");
    }
}
