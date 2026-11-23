package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.assessmentgrading.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewAiQualityReportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewAssignableTeachersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewClassTestGradingResultsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewClassTestGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewGradingAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewGradingTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyClassTestGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyGradingAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
import com.sep.vox.application.query.dto.GradingTurnInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends grading queries through the real schema and asserts the typed values: the {@code DateTime} scalar, the
 * {@code AssessmentCandidateResultStatus} and {@code TurnType} enums on the output, the
 * {@code AssessmentCandidateResultStatus} variable bound as an enum, and an unknown value rejected before the use case runs.
 */
@GraphQLControllerTest(GraphQLGradingController.class)
@MockitoBean(types = {
    ViewClassTestGradingStatsUseCase.class, ViewMyGradingAssessmentsUseCase.class,
    ViewAssignableTeachersUseCase.class, ViewAiQualityReportUseCase.class })
class GradingGraphQLTests {

    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_RESULT_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final Instant ASSIGNED_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant DEADLINE_AT = Instant.parse("2026-08-05T10:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase;
    @MockitoBean
    private ViewGradingStatsUseCase viewGradingStatsUseCase;
    @MockitoBean
    private ViewClassTestGradingResultsUseCase viewClassTestGradingResultsUseCase;
    @MockitoBean
    private ViewMyGradingTasksUseCase viewMyGradingTasksUseCase;
    @MockitoBean
    private ViewMyClassTestGradingTasksUseCase viewMyClassTestGradingTasksUseCase;
    @MockitoBean
    private ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase;
    @MockitoBean
    private ViewResultStatusHistoryUseCase viewResultStatusHistoryUseCase;

    private static final String ASSIGNMENTS_QUERY = """
        query($resultStatus: AssessmentCandidateResultStatus) {
            gradingAssignments(resultStatus: $resultStatus) {
                content { resultStatus assignedAt completedAt deadlineAt }
            }
        }""";

    private static GradingAssignmentRowInfo assignmentRow() {
        return new GradingAssignmentRowInfo(CANDIDATE_RESULT_ID, "A2041F3C", "Lan", "10A1", "Midterm",
            AssessmentCandidateResultStatus.PENDING_REVIEW, BigDecimal.valueOf(6.5), false, ASSIGNMENT_ID,
            UUID.randomUUID(), "Cô Hoa", "INITIAL", "ASSIGNED", null, ASSIGNED_AT, null, DEADLINE_AT, false, false,
            UUID.randomUUID(), 1, 1);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assignments_should_return_the_status_enum_and_date_time_values() {
        when(viewGradingAssignmentsUseCase.execute(any()))
            .thenReturn(new PageResult<GradingAssignmentRowInfo>(List.of(assignmentRow()), 1, 20, 1, 1));

        graphQlTester.document(ASSIGNMENTS_QUERY).execute()
            .errors().verify()
            .path("gradingAssignments.content[0].resultStatus").entity(String.class).isEqualTo("PENDING_REVIEW")
            .path("gradingAssignments.content[0].assignedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("gradingAssignments.content[0].completedAt").valueIsNull()
            .path("gradingAssignments.content[0].deadlineAt").entity(String.class).isEqualTo("2026-08-05T10:00:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assignments_should_bind_the_result_status_variable_as_an_enum() {
        when(viewGradingAssignmentsUseCase.execute(any()))
            .thenReturn(new PageResult<GradingAssignmentRowInfo>(List.of(), 1, 20, 0, 0));

        graphQlTester.document(ASSIGNMENTS_QUERY).variable("resultStatus", "RELEASED").execute().errors().verify();

        verify(viewGradingAssignmentsUseCase).execute(new SearchGradingAssignmentsQuery(
            null, null, null, AssessmentCandidateResultStatus.RELEASED, null, null, false, false, null, null, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assignments_should_reject_an_unknown_result_status_as_a_validation_error() {
        graphQlTester.document("{ gradingAssignments(resultStatus: NOT_A_STATUS) { page } }").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewGradingAssignmentsUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void class_test_results_should_bind_the_result_status_variable_as_an_enum() {
        when(viewClassTestGradingResultsUseCase.execute(any()))
            .thenReturn(new PageResult<GradingAssignmentRowInfo>(List.of(), 1, 20, 0, 0));

        graphQlTester.document("""
            query($resultStatus: AssessmentCandidateResultStatus) {
                classTestGradingResults(assessmentId: "%s", resultStatus: $resultStatus) { page }
            }""".formatted(ASSESSMENT_ID)).variable("resultStatus", "FINAL").execute().errors().verify();

        verify(viewClassTestGradingResultsUseCase).execute(new ViewClassTestGradingResultsQuery(
            ASSESSMENT_ID, AssessmentCandidateResultStatus.FINAL, false, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_tasks_should_return_the_result_status_enum_and_date_time_values() {
        GradingTaskInfo task = new GradingTaskInfo(ASSIGNMENT_ID, CANDIDATE_RESULT_ID, "A2041F3C", "Midterm", 2,
            "SPOT_CHECK", "ASSIGNED", AssessmentCandidateResultStatus.RELEASED, BigDecimal.valueOf(7), false,
            ASSIGNED_AT, DEADLINE_AT, false, null, null, UUID.randomUUID(), 1, 1);
        when(viewMyGradingTasksUseCase.execute(any()))
            .thenReturn(new PageResult<GradingTaskInfo>(List.of(task), 1, 20, 1, 1));

        graphQlTester.document("{ myGradingTasks { content { resultStatus assignedAt deadlineAt } } }").execute()
            .errors().verify()
            .path("myGradingTasks.content[0].resultStatus").entity(String.class).isEqualTo("RELEASED")
            .path("myGradingTasks.content[0].assignedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("myGradingTasks.content[0].deadlineAt").entity(String.class).isEqualTo("2026-08-05T10:00:00Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void task_detail_should_return_the_result_status_deadline_and_turn_type() {
        GradingTurnInfo turn = new GradingTurnInfo(UUID.randomUUID(), 1, TurnType.FOLLOWUP, null, null, null, null,
            null, null, null, null);
        GradingTaskItemInfo item = new GradingTaskItemInfo(UUID.randomUUID(), UUID.randomUUID(), "Part 1", null, 1,
            null, null, null, null, List.of(), List.of(turn), List.of(), null, null, false, null, false, false, null,
            null);
        GradingTaskDetailInfo detail = new GradingTaskDetailInfo(ASSIGNMENT_ID, CANDIDATE_RESULT_ID, "A2041F3C",
            "Midterm", "INITIAL", "ASSIGNED", AssessmentCandidateResultStatus.PENDING_REVIEW, false, null, null, null,
            DEADLINE_AT, false, true, List.of("UPHELD"), null, List.of(item), List.of(), null, null, null, 1, 1);
        when(viewGradingTaskDetailUseCase.execute(ASSIGNMENT_ID)).thenReturn(detail);

        graphQlTester.document("""
            { gradingTaskDetail(assignmentId: "%s") { resultStatus deadlineAt items { turns { turnType } } } }"""
            .formatted(ASSIGNMENT_ID)).execute()
            .errors().verify()
            .path("gradingTaskDetail.resultStatus").entity(String.class).isEqualTo("PENDING_REVIEW")
            .path("gradingTaskDetail.deadlineAt").entity(String.class).isEqualTo("2026-08-05T10:00:00Z")
            .path("gradingTaskDetail.items[0].turns[0].turnType").entity(String.class).isEqualTo("FOLLOWUP");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void status_history_should_return_enum_statuses_and_a_date_time() {
        ResultStatusHistoryInfo first = new ResultStatusHistoryInfo(UUID.randomUUID(), CANDIDATE_RESULT_ID, null,
            AssessmentCandidateResultStatus.PENDING_REVIEW, null, null, "AI_EVALUATION", null, null, null, ASSIGNED_AT);
        ResultStatusHistoryInfo second = new ResultStatusHistoryInfo(UUID.randomUUID(), CANDIDATE_RESULT_ID,
            AssessmentCandidateResultStatus.PENDING_REVIEW, AssessmentCandidateResultStatus.RELEASED, null, null,
            "TEACHER_INITIAL", null, null, null, DEADLINE_AT);
        when(viewResultStatusHistoryUseCase.execute(CANDIDATE_RESULT_ID)).thenReturn(List.of(first, second));

        graphQlTester.document("""
            { resultStatusHistory(candidateResultId: "%s") { fromStatus toStatus createdAt } }"""
            .formatted(CANDIDATE_RESULT_ID)).execute()
            .errors().verify()
            .path("resultStatusHistory[0].fromStatus").valueIsNull()
            .path("resultStatusHistory[0].toStatus").entity(String.class).isEqualTo("PENDING_REVIEW")
            .path("resultStatusHistory[1].fromStatus").entity(String.class).isEqualTo("PENDING_REVIEW")
            .path("resultStatusHistory[1].toStatus").entity(String.class).isEqualTo("RELEASED")
            .path("resultStatusHistory[1].createdAt").entity(String.class).isEqualTo("2026-08-05T10:00:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void stats_should_return_the_status_enum_in_each_count() {
        GradingStatsInfo stats = new GradingStatsInfo(3,
            List.of(new GradingStatsInfo.ResultStatusCount(AssessmentCandidateResultStatus.APPEALED, 3)), 0, 0, 0,
            List.of());
        when(viewGradingStatsUseCase.execute(any())).thenReturn(stats);

        graphQlTester.document("{ gradingStats { byResultStatus { status count } } }").execute()
            .errors().verify()
            .path("gradingStats.byResultStatus[0].status").entity(String.class).isEqualTo("APPEALED")
            .path("gradingStats.byResultStatus[0].count").entity(Integer.class).isEqualTo(3);
    }
}
