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

import com.sep.vox.application.port.input.query.SearchAppealsByAssessmentQuery;
import com.sep.vox.application.port.input.query.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealsByAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewMyAppealsUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealItemInfo;
import com.sep.vox.application.query.dto.AppealReviewerInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.dto.AppealTurnInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends appeal queries through the real schema and asserts the typed values: the {@code DateTime} scalar, the
 * {@code AppealStatus}, {@code GradingAssignmentStatus}, {@code GradingOutcome} and {@code TurnType} enums on the
 * output, the {@code AppealStatus} variable bound to the Java enum, and an unknown value rejected before the use case runs.
 */
@GraphQlControllerTest(AssessmentAppealController.class)
@MockitoBean(types = {
    ViewAssessmentAppealStatsUseCase.class, ViewAssignableReviewersUseCase.class, ViewMyAppealDetailUseCase.class })
class AssessmentAppealGraphQlTests {

    private static final UUID APPEAL_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final Instant REQUESTED_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-05T10:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentAppealsUseCase viewAssessmentAppealsUseCase;
    @MockitoBean
    private ViewAssessmentAppealsByAssessmentUseCase viewAssessmentAppealsByAssessmentUseCase;
    @MockitoBean
    private ViewAssessmentAppealDetailUseCase viewAssessmentAppealDetailUseCase;
    @MockitoBean
    private ViewMyAppealsUseCase viewMyAppealsUseCase;

    private static AppealSummaryInfo summary() {
        return new AppealSummaryInfo(APPEAL_ID, "Lan", "10A1", "Midterm", List.of("Part 1"), BigDecimal.valueOf(6.5),
            AssessmentAppealStatus.GRADING, REQUESTED_AT, DEADLINE, "Cô Hoa", GradingAssignmentStatus.ASSIGNED, false);
    }

    private static PageResult<AppealSummaryInfo> pageOf(List<AppealSummaryInfo> content) {
        return new PageResult<AppealSummaryInfo>(content, 1, 20, content.size(), 1);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void appeals_should_return_enum_statuses_and_date_time_values() {
        when(viewAssessmentAppealsUseCase.execute(any())).thenReturn(pageOf(List.of(summary())));

        graphQlTester.document("{ appeals { content { status reviewerStatus requestedAt deadline } } }").execute()
            .errors().verify()
            .path("appeals.content[0].status").entity(String.class).isEqualTo("GRADING")
            .path("appeals.content[0].reviewerStatus").entity(String.class).isEqualTo("ASSIGNED")
            .path("appeals.content[0].requestedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("appeals.content[0].deadline").entity(String.class).isEqualTo("2026-08-05T10:00:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void appeals_should_bind_the_status_variable_as_an_enum() {
        when(viewAssessmentAppealsUseCase.execute(any())).thenReturn(pageOf(List.of()));

        graphQlTester.document("query($status: AppealStatus) { appeals(status: $status) { page } }")
            .variable("status", "WITHDRAWN").execute().errors().verify();

        verify(viewAssessmentAppealsUseCase).execute(
            new SearchAssessmentAppealsQuery(AssessmentAppealStatus.WITHDRAWN, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void appeals_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ appeals(status: NOT_A_STATUS) { page } }").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewAssessmentAppealsUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_appeals_should_bind_the_status_variable_as_an_enum() {
        when(viewAssessmentAppealsByAssessmentUseCase.execute(any())).thenReturn(pageOf(List.of()));

        graphQlTester.document("""
            query($status: AppealStatus) {
                assessmentAppeals(assessmentId: "%s", status: $status) { page }
            }""".formatted(ASSESSMENT_ID)).variable("status", "PENDING").execute().errors().verify();

        verify(viewAssessmentAppealsByAssessmentUseCase).execute(
            new SearchAppealsByAssessmentQuery(ASSESSMENT_ID, AssessmentAppealStatus.PENDING, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_appeals_should_bind_the_status_variable_as_an_enum() {
        when(viewMyAppealsUseCase.execute(any())).thenReturn(pageOf(List.of()));

        graphQlTester.document("query($status: AppealStatus) { myAppeals(status: $status) { page } }")
            .variable("status", "REJECTED").execute().errors().verify();

        verify(viewMyAppealsUseCase).execute(new SearchAssessmentAppealsQuery(AssessmentAppealStatus.REJECTED, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void appeal_detail_should_return_date_time_values_reviewer_enums_and_the_turn_type() {
        AppealTurnInfo turn = new AppealTurnInfo(UUID.randomUUID(), 1, TurnType.FOLLOWUP, null, null, null, null);
        AppealItemInfo item = new AppealItemInfo(UUID.randomUUID(), UUID.randomUUID(), "Part 1", List.of(),
            List.of(turn), null);
        AppealReviewerInfo reviewer = new AppealReviewerInfo(UUID.randomUUID(), UUID.randomUUID(), "Cô Hoa",
            GradingAssignmentStatus.COMPLETED, GradingOutcome.REGRADED, REQUESTED_AT, DEADLINE, null, false);
        AppealDetailInfo detail = new AppealDetailInfo(APPEAL_ID, "Lan", "10A1", "Midterm", BigDecimal.valueOf(6.5),
            AssessmentAppealStatus.PUBLISHED, REQUESTED_AT, DEADLINE, "reason", null, null, BigDecimal.valueOf(7),
            REQUESTED_AT, DEADLINE, null, null, List.of(item), reviewer, false, BigDecimal.ZERO, BigDecimal.TEN);
        when(viewAssessmentAppealDetailUseCase.execute(APPEAL_ID)).thenReturn(detail);

        graphQlTester.document("""
            { appeal(id: "%s") {
                status requestedAt deadline approvedAt resolvedAt withdrawnAt
                items { turns { turnType } }
                reviewer { status outcome assignedAt completedAt deadlineAt }
            } }""".formatted(APPEAL_ID)).execute()
            .errors().verify()
            .path("appeal.status").entity(String.class).isEqualTo("PUBLISHED")
            .path("appeal.requestedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("appeal.approvedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("appeal.resolvedAt").entity(String.class).isEqualTo("2026-08-05T10:00:00Z")
            .path("appeal.withdrawnAt").valueIsNull()
            .path("appeal.items[0].turns[0].turnType").entity(String.class).isEqualTo("FOLLOWUP")
            .path("appeal.reviewer.status").entity(String.class).isEqualTo("COMPLETED")
            .path("appeal.reviewer.outcome").entity(String.class).isEqualTo("REGRADED")
            .path("appeal.reviewer.assignedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("appeal.reviewer.completedAt").entity(String.class).isEqualTo("2026-08-05T10:00:00Z")
            .path("appeal.reviewer.deadlineAt").valueIsNull();
    }
}
