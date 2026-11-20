package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.ViewGradingFailureOverviewQuery;
import com.sep.vox.application.port.input.query.ViewGradingFailureSessionsQuery;
import com.sep.vox.application.port.input.query.ViewPlatformBusinessHealthQuery;
import com.sep.vox.application.port.input.query.ViewPlatformOperationalHealthQuery;
import com.sep.vox.application.port.input.query.ViewSchoolAiCostQuery;
import com.sep.vox.application.port.input.query.ViewSchoolAiCostQuery.AiCostGranularity;
import com.sep.vox.application.port.input.query.SearchSchoolAiSpendByUserQuery;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureOverviewUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureSessionsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewNearestCentralizedAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformBusinessHealthUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewQuestionBankStatsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAiCostTimeseriesUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAiSpendByUserUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolGradingFailuresUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolsAtRiskUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewTeacherDashboardUseCase;
import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.NearestCentralizedAssessmentDto;
import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.application.response.input.dashboard.AssessmentAppealStatsResponse;
import com.sep.vox.application.response.input.dashboard.AssessmentAwaitingPublishResponse;
import com.sep.vox.application.response.input.dashboard.AssessmentStatusCountResponse;
import com.sep.vox.application.response.input.dashboard.GradingFailureOverviewResponse;
import com.sep.vox.application.response.input.dashboard.GradingOutcomeBucketResponse;
import com.sep.vox.application.response.input.dashboard.PlatformBusinessHealthResponse;
import com.sep.vox.application.response.input.dashboard.PlatformOperationalHealthResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAiCostTimeseriesResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAiCostTimeseriesResponse.AiCostPointResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAiSpendByUserPageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolFundingResponse;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailurePageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailureResponse;
import com.sep.vox.application.response.input.dashboard.SchoolSubscriptionRenewalResponse;
import com.sep.vox.application.response.input.dashboard.SchoolsAtRiskResponse;
import com.sep.vox.application.response.input.dashboard.UnscoredWorkloadResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends dashboard queries through the real schema and asserts the typed values: the {@code DateTime} and {@code Date}
 * scalars on the output, {@code DateTime} variables delivered to the use case as {@link Instant}, the
 * {@code AiCostGranularity} enum, and an offset-less date rejected before the use case runs.
 */
@GraphQlControllerTest(DashboardController.class)
@MockitoBean(types = {
    ViewSystemAdminDashboardUseCase.class, ViewTeacherDashboardUseCase.class, ViewQuestionBankStatsUseCase.class })
class DashboardGraphQlTests {

    private static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-15T10:30:00Z");
    private static final Instant FAILED_AT = Instant.parse("2026-07-14T03:20:00Z");
    private static final String RANGE_VARIABLES_QUERY_TEMPLATE = "query($from: DateTime, $to: DateTime) { %s }";

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase;
    @MockitoBean
    private ViewNearestCentralizedAssessmentUseCase viewNearestCentralizedAssessmentUseCase;
    @MockitoBean
    private ViewPlatformOperationalHealthUseCase viewPlatformOperationalHealthUseCase;
    @MockitoBean
    private ViewPlatformBusinessHealthUseCase viewPlatformBusinessHealthUseCase;
    @MockitoBean
    private ViewGradingFailureOverviewUseCase viewGradingFailureOverviewUseCase;
    @MockitoBean
    private ViewGradingFailureSessionsUseCase viewGradingFailureSessionsUseCase;
    @MockitoBean
    private ViewSchoolsAtRiskUseCase viewSchoolsAtRiskUseCase;
    @MockitoBean
    private ViewSchoolGradingFailuresUseCase viewSchoolGradingFailuresUseCase;
    @MockitoBean
    private ViewSchoolAiCostTimeseriesUseCase viewSchoolAiCostTimeseriesUseCase;
    @MockitoBean
    private ViewSchoolAiSpendByUserUseCase viewSchoolAiSpendByUserUseCase;

    private static String rangeQuery(String selection) {
        return RANGE_VARIABLES_QUERY_TEMPLATE.formatted(selection);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void operational_health_should_deliver_instants_and_return_calendar_days_as_dates() {
        when(viewPlatformOperationalHealthUseCase.execute(any())).thenReturn(new PlatformOperationalHealthResponse(
            1, 1, 0, 9, 1, 90.0, List.of(new GradingOutcomeBucketResponse(LocalDate.parse("2026-07-14"), 9, 1))));

        graphQlTester.document(rangeQuery("platformOperationalHealth(dateFrom: $from, dateTo: $to) { daily { day graded } }"))
            .variable("from", "2026-07-01T00:00:00Z").variable("to", "2026-07-15T17:30:00+07:00").execute()
            .errors().verify()
            .path("platformOperationalHealth.daily[0].day").entity(String.class).isEqualTo("2026-07-14");

        verify(viewPlatformOperationalHealthUseCase).execute(new ViewPlatformOperationalHealthQuery(FROM, TO));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void operational_health_should_reject_a_date_without_an_offset() {
        graphQlTester.document(rangeQuery("platformOperationalHealth(dateFrom: $from, dateTo: $to) { graded }"))
            .variable("from", "2026-07-01").variable("to", null).execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("from"))
            .verify();

        verifyNoInteractions(viewPlatformOperationalHealthUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void business_health_should_deliver_instants_to_the_query() {
        when(viewPlatformBusinessHealthUseCase.execute(any())).thenReturn(new PlatformBusinessHealthResponse(
            1, 0, 0, 0, 0, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, 50.0, null));

        graphQlTester.document(rangeQuery("platformBusinessHealth(dateFrom: $from, dateTo: $to) { subscribedSchools }"))
            .variable("from", "2026-07-01T00:00:00Z").variable("to", "2026-07-15T10:30:00Z").execute().errors().verify();

        verify(viewPlatformBusinessHealthUseCase).execute(new ViewPlatformBusinessHealthQuery(FROM, TO));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void failure_overview_should_deliver_instants_and_return_group_times_as_date_time() {
        GradingFailureGroupDto group = new GradingFailureGroupDto("sig", "boom", 2, 1, 1, FAILED_AT, TO, 2);
        when(viewGradingFailureOverviewUseCase.execute(any())).thenReturn(
            new GradingFailureOverviewResponse(2, 1, 1, 2, List.of(group), 0));

        graphQlTester.document(rangeQuery(
                "gradingFailureOverview(dateFrom: $from, dateTo: $to) { groups { firstFailedAt lastFailedAt } }"))
            .variable("from", "2026-07-01T00:00:00Z").variable("to", "2026-07-15T10:30:00Z").execute()
            .errors().verify()
            .path("gradingFailureOverview.groups[0].firstFailedAt").entity(String.class).isEqualTo("2026-07-14T03:20:00Z")
            .path("gradingFailureOverview.groups[0].lastFailedAt").entity(String.class).isEqualTo("2026-07-15T10:30:00Z");

        verify(viewGradingFailureOverviewUseCase).execute(new ViewGradingFailureOverviewQuery(FROM, TO));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void failure_sessions_should_deliver_instants_and_return_failed_at_as_date_time() {
        GradingFailureSessionDto session = new GradingFailureSessionDto(UUID.randomUUID(), null, null, null,
            UUID.randomUUID(), "Midterm", "Lan", FAILED_AT, 2, "boom", true, false);
        when(viewGradingFailureSessionsUseCase.execute(any()))
            .thenReturn(new PageResult<GradingFailureSessionDto>(List.of(session), 1, 20, 1, 1));

        graphQlTester.document(rangeQuery(
                "gradingFailureSessions(dateFrom: $from, dateTo: $to) { content { failedAt } }"))
            .variable("from", "2026-07-01T00:00:00Z").variable("to", null).execute()
            .errors().verify()
            .path("gradingFailureSessions.content[0].failedAt").entity(String.class).isEqualTo("2026-07-14T03:20:00Z");

        verify(viewGradingFailureSessionsUseCase).execute(new ViewGradingFailureSessionsQuery(FROM, null, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void ai_cost_should_return_typed_times_and_the_granularity_enum() {
        when(viewSchoolAiCostTimeseriesUseCase.execute(any())).thenReturn(new SchoolAiCostTimeseriesResponse(
            AiCostGranularity.WEEK, "12.500000",
            List.of(new AiCostPointResponse(Instant.parse("2026-07-12T17:00:00Z"), "ASSESSMENT", "12.500000")), FROM));

        graphQlTester.document(rangeQuery("""
                schoolAiCostTimeseries(dateFrom: $from, dateTo: $to, granularity: WEEK) {
                    granularity recordedFrom points { bucket }
                }"""))
            .variable("from", "2026-07-01T00:00:00Z").variable("to", "2026-07-15T10:30:00Z").execute()
            .errors().verify()
            .path("schoolAiCostTimeseries.granularity").entity(String.class).isEqualTo("WEEK")
            .path("schoolAiCostTimeseries.recordedFrom").entity(String.class).isEqualTo("2026-07-01T00:00:00Z")
            .path("schoolAiCostTimeseries.points[0].bucket").entity(String.class).isEqualTo("2026-07-12T17:00:00Z");

        verify(viewSchoolAiCostTimeseriesUseCase).execute(new ViewSchoolAiCostQuery(FROM, TO, AiCostGranularity.WEEK));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void ai_cost_should_reject_a_date_without_an_offset() {
        graphQlTester.document(rangeQuery("schoolAiCostTimeseries(dateFrom: $from, dateTo: $to) { totalCostVnd }"))
            .variable("from", "2026-07-01T00:00:00").variable("to", null).execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("from"))
            .verify();

        verifyNoInteractions(viewSchoolAiCostTimeseriesUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void ai_spend_by_user_should_deliver_instants_to_the_query() {
        when(viewSchoolAiSpendByUserUseCase.execute(any()))
            .thenReturn(new SchoolAiSpendByUserPageResponse(List.of(), 1, 20, 0, 0, "0.000000"));

        graphQlTester.document(rangeQuery(
                "schoolAiSpendByUser(dateFrom: $from, dateTo: $to, quotaType: ASSESSMENT) { totalElements }"))
            .variable("from", "2026-07-01T00:00:00Z").variable("to", "2026-07-15T10:30:00Z").execute().errors().verify();

        verify(viewSchoolAiSpendByUserUseCase)
            .execute(new SearchSchoolAiSpendByUserQuery(FROM, TO, QuotaType.ASSESSMENT, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_dashboard_should_return_renewal_and_publish_times_as_date_time() {
        when(viewSchoolAdminDashboardUseCase.execute(any())).thenReturn(schoolDashboard());

        graphQlTester.document("""
                { schoolAdminDashboard {
                    subscriptionRenewal { endDate }
                    assessmentsAwaitingPublish { closeAt }
                } }""").execute()
            .errors().verify()
            .path("schoolAdminDashboard.subscriptionRenewal.endDate").entity(String.class).isEqualTo("2027-07-28T02:30:00Z")
            .path("schoolAdminDashboard.assessmentsAwaitingPublish[0].closeAt").entity(String.class).isEqualTo("2026-07-15T10:30:00Z")
            .path("schoolAdminDashboard.assessmentsAwaitingPublish[1].closeAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_grading_failures_should_return_failed_at_as_date_time() {
        SchoolGradingFailureResponse failure = new SchoolGradingFailureResponse(UUID.randomUUID(), UUID.randomUUID(),
            "A1", "Midterm", "Lan", "10A1", FAILED_AT, "boom", 1, true);
        when(viewSchoolGradingFailuresUseCase.execute(any()))
            .thenReturn(new SchoolGradingFailurePageResponse(List.of(failure), 1, 20, 1, 1, 1, 0));

        graphQlTester.document("{ schoolGradingFailures { content { failedAt } } }").execute()
            .errors().verify()
            .path("schoolGradingFailures.content[0].failedAt").entity(String.class).isEqualTo("2026-07-14T03:20:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void nearest_assessment_should_return_open_and_close_times_as_date_time() {
        when(viewNearestCentralizedAssessmentUseCase.execute(any())).thenReturn(Optional.of(
            new NearestCentralizedAssessmentDto(UUID.randomUUID(), "A1", "Midterm", "SCHEDULED", FROM, TO, 30, 2)));

        graphQlTester.document("{ nearestCentralizedAssessment { status openAt closeAt } }").execute()
            .errors().verify()
            .path("nearestCentralizedAssessment.status").entity(String.class).isEqualTo("SCHEDULED")
            .path("nearestCentralizedAssessment.openAt").entity(String.class).isEqualTo("2026-07-01T00:00:00Z")
            .path("nearestCentralizedAssessment.closeAt").entity(String.class).isEqualTo("2026-07-15T10:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void schools_at_risk_should_return_the_period_end_as_date_time() {
        SchoolAtRiskDto school = new SchoolAtRiskDto(UUID.randomUUID(), "School", "S1", "Plan", TO, null, BigDecimal.ZERO);
        when(viewSchoolsAtRiskUseCase.execute(any())).thenReturn(new SchoolsAtRiskResponse(
            SchoolRiskBucket.EXPIRING_SOON, new SchoolsAtRiskResponse.BucketCounts(1, 0, 0, 0),
            new PageResult<SchoolAtRiskDto>(List.of(school), 1, 20, 1, 1)));

        graphQlTester.document("{ schoolsAtRisk(bucket: EXPIRING_SOON) { schools { content { relevantEndDate } } } }")
            .execute().errors().verify()
            .path("schoolsAtRisk.schools.content[0].relevantEndDate").entity(String.class).isEqualTo("2026-07-15T10:30:00Z");
    }

    private static SchoolAdminDashboardSummaryResponse schoolDashboard() {
        return new SchoolAdminDashboardSummaryResponse(
            new AssessmentStatusCountResponse(0, 0, 0, 0, 0, 0, 0),
            new AssessmentAppealStatsResponse(0, 0, 0, 0, 0),
            BigDecimal.ZERO, List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
            new SchoolSubscriptionRenewalResponse("Plan", "ACTIVE", Instant.parse("2027-07-28T02:30:00Z")),
            new SchoolFundingResponse("0", "0", "0", false, "0", "0", "0"),
            new UnscoredWorkloadResponse(0, 0, 0, 0, 0, 0, 0, null, 0),
            List.of(
                new AssessmentAwaitingPublishResponse(UUID.randomUUID(), "A1", "Midterm", TO, 1, 1, 0, 0),
                new AssessmentAwaitingPublishResponse(UUID.randomUUID(), "A2", "Final", null, 1, 1, 0, 0)),
            null);
    }
}
