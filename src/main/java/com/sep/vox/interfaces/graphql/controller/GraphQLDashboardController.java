package com.sep.vox.interfaces.graphql.controller;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.dashboard.ViewGradingFailureOverviewQuery;
import com.sep.vox.application.port.input.query.dashboard.ViewGradingFailureSessionsQuery;
import com.sep.vox.application.port.input.query.dashboard.ViewPlatformBusinessHealthQuery;
import com.sep.vox.application.port.input.query.dashboard.ViewPlatformOperationalHealthQuery;
import com.sep.vox.application.port.input.query.dashboard.SearchSchoolAiSpendByUserQuery;
import com.sep.vox.application.port.input.query.dashboard.SearchSchoolGradingFailuresQuery;
import com.sep.vox.application.port.input.query.dashboard.ViewSchoolAiCostQuery;
import com.sep.vox.application.port.input.query.dashboard.ViewSchoolAiCostQuery.AiCostGranularity;
import com.sep.vox.application.port.input.query.dashboard.ViewSchoolsAtRiskQuery;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureOverviewUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureSessionsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewNearestCentralizedAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformBusinessHealthUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAiCostTimeseriesUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAiSpendByUserUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolGradingFailuresUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolsAtRiskUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewQuestionBankStatsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewTeacherDashboardUseCase;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.NearestCentralizedAssessmentDto;
import com.sep.vox.application.query.dto.QuestionBankStatsDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.application.response.input.dashboard.GradingFailureOverviewResponse;
import com.sep.vox.application.response.input.dashboard.PlatformBusinessHealthResponse;
import com.sep.vox.application.response.input.dashboard.PlatformOperationalHealthResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAiCostTimeseriesResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAiSpendByUserPageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailurePageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolsAtRiskResponse;
import com.sep.vox.application.response.input.dashboard.SystemAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.TeacherDashboardSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.shared.PageArguments;

@Controller
@RequiredArgsConstructor
public class GraphQLDashboardController {

    private final ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase;
    private final ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase;
    private final ViewTeacherDashboardUseCase viewTeacherDashboardUseCase;
    private final ViewNearestCentralizedAssessmentUseCase viewNearestCentralizedAssessmentUseCase;
    private final ViewQuestionBankStatsUseCase viewQuestionBankStatsUseCase;
    private final ViewPlatformOperationalHealthUseCase viewPlatformOperationalHealthUseCase;
    private final ViewPlatformBusinessHealthUseCase viewPlatformBusinessHealthUseCase;
    private final ViewGradingFailureOverviewUseCase viewGradingFailureOverviewUseCase;
    private final ViewGradingFailureSessionsUseCase viewGradingFailureSessionsUseCase;
    private final ViewSchoolsAtRiskUseCase viewSchoolsAtRiskUseCase;
    private final ViewSchoolGradingFailuresUseCase viewSchoolGradingFailuresUseCase;
    private final ViewSchoolAiCostTimeseriesUseCase viewSchoolAiCostTimeseriesUseCase;
    private final ViewSchoolAiSpendByUserUseCase viewSchoolAiSpendByUserUseCase;

    @QueryMapping(name = "systemAdminDashboard")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public SystemAdminDashboardSummaryResponse systemAdminDashboard() {
        return viewSystemAdminDashboardUseCase.execute(null);
    }

    @QueryMapping(name = "schoolAdminDashboard")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolAdminDashboardSummaryResponse schoolAdminDashboard() {
        return viewSchoolAdminDashboardUseCase.execute(null);
    }

    /**
     * Landing spot of the "AI grading failed, nobody has handled it yet" row on the school overview page.
     *
     * <p>Takes no schoolId: the use case derives the scope from the logged-in user, so a school admin
     * has no way to read another school's results through this query.
     */
    @QueryMapping(name = "schoolGradingFailures")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolGradingFailurePageResponse schoolGradingFailures(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "retryLeft") Boolean retryLeft,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolGradingFailuresUseCase.execute(
            new SearchSchoolGradingFailuresQuery(assessmentId, retryLeft, page, size));
    }

    /**
     * AI cost of the logged-in school over time.
     *
     * <p>It takes no schoolId, unlike the {@code schoolTokenUsageTimeseries} that clients still call: the use case
     * takes the scope from the logged-in user, so there is no way left for a school admin to read another
     * school's spending.
     */
    @QueryMapping(name = "schoolAiCostTimeseries")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolAiCostTimeseriesResponse schoolAiCostTimeseries(
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo,
            @Argument(name = "granularity") AiCostGranularity granularity) {
        return viewSchoolAiCostTimeseriesUseCase.execute(new ViewSchoolAiCostQuery(
            dateFrom, dateTo, granularity));
    }

    @QueryMapping(name = "schoolAiSpendByUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolAiSpendByUserPageResponse schoolAiSpendByUser(
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo,
            @Argument(name = "quotaType") QuotaType quotaType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolAiSpendByUserUseCase.execute(new SearchSchoolAiSpendByUserQuery(
            dateFrom, dateTo, quotaType, page, size));
    }

    @QueryMapping(name = "teacherDashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherDashboardSummaryResponse teacherDashboard() {
        return viewTeacherDashboardUseCase.execute(null);
    }

    @QueryMapping(name = "nearestCentralizedAssessment")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public NearestCentralizedAssessmentDto nearestCentralizedAssessment() {
        return viewNearestCentralizedAssessmentUseCase.execute(null).orElse(null);
    }

    @QueryMapping(name = "questionBankStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public QuestionBankStatsDto questionBankStats() {
        return viewQuestionBankStatsUseCase.execute(null);
    }

    @QueryMapping(name = "platformOperationalHealth")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PlatformOperationalHealthResponse platformOperationalHealth(
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo) {
        return viewPlatformOperationalHealthUseCase.execute(new ViewPlatformOperationalHealthQuery(
            dateFrom,
            dateTo
        ));
    }

    @QueryMapping(name = "platformBusinessHealth")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PlatformBusinessHealthResponse platformBusinessHealth(
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo) {
        return viewPlatformBusinessHealthUseCase.execute(new ViewPlatformBusinessHealthQuery(
            dateFrom,
            dateTo
        ));
    }

    @QueryMapping(name = "gradingFailureOverview")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public GradingFailureOverviewResponse gradingFailureOverview(
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo) {
        return viewGradingFailureOverviewUseCase.execute(new ViewGradingFailureOverviewQuery(
            dateFrom,
            dateTo
        ));
    }

    @QueryMapping(name = "gradingFailureSessions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<GradingFailureSessionDto> gradingFailureSessions(
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo,
            @Argument(name = "signature") String signature,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewGradingFailureSessionsUseCase.execute(new ViewGradingFailureSessionsQuery(
            dateFrom,
            dateTo,
            signature,
            page,
            size
        ));
    }

    @QueryMapping(name = "schoolsAtRisk")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public SchoolsAtRiskResponse schoolsAtRisk(
            @Argument(name = "bucket") SchoolRiskBucket bucket,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolsAtRiskUseCase.execute(new ViewSchoolsAtRiskQuery(bucket, keyword, page, size));
    }

}
