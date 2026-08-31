package com.sep.vox.interfaces.graphql.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.ViewGradingFailureOverviewQuery;
import com.sep.vox.application.port.input.query.ViewGradingFailureSessionsQuery;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureOverviewUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureSessionsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewNearestCentralizedExamUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformBusinessHealthUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewQuestionBankStatsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewTeacherDashboardUseCase;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.NearestCentralizedExamDto;
import com.sep.vox.application.query.dto.QuestionBankStatsDto;
import com.sep.vox.application.response.input.dashboard.GradingFailureOverviewResponse;
import com.sep.vox.application.response.input.dashboard.PlatformBusinessHealthResponse;
import com.sep.vox.application.response.input.dashboard.PlatformOperationalHealthResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SystemAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.TeacherDashboardSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.shared.PageArguments;

@Controller
public class DashboardController {

    private final ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase;
    private final ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase;
    private final ViewTeacherDashboardUseCase viewTeacherDashboardUseCase;
    private final ViewNearestCentralizedExamUseCase viewNearestCentralizedExamUseCase;
    private final ViewQuestionBankStatsUseCase viewQuestionBankStatsUseCase;
    private final ViewPlatformOperationalHealthUseCase viewPlatformOperationalHealthUseCase;
    private final ViewPlatformBusinessHealthUseCase viewPlatformBusinessHealthUseCase;
    private final ViewGradingFailureOverviewUseCase viewGradingFailureOverviewUseCase;
    private final ViewGradingFailureSessionsUseCase viewGradingFailureSessionsUseCase;

    public DashboardController(ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase,
            ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase,
            ViewTeacherDashboardUseCase viewTeacherDashboardUseCase,
            ViewNearestCentralizedExamUseCase viewNearestCentralizedExamUseCase,
            ViewQuestionBankStatsUseCase viewQuestionBankStatsUseCase,
            ViewPlatformOperationalHealthUseCase viewPlatformOperationalHealthUseCase,
            ViewPlatformBusinessHealthUseCase viewPlatformBusinessHealthUseCase,
            ViewGradingFailureOverviewUseCase viewGradingFailureOverviewUseCase,
            ViewGradingFailureSessionsUseCase viewGradingFailureSessionsUseCase) {
        this.viewSystemAdminDashboardUseCase = viewSystemAdminDashboardUseCase;
        this.viewSchoolAdminDashboardUseCase = viewSchoolAdminDashboardUseCase;
        this.viewTeacherDashboardUseCase = viewTeacherDashboardUseCase;
        this.viewNearestCentralizedExamUseCase = viewNearestCentralizedExamUseCase;
        this.viewQuestionBankStatsUseCase = viewQuestionBankStatsUseCase;
        this.viewPlatformOperationalHealthUseCase = viewPlatformOperationalHealthUseCase;
        this.viewPlatformBusinessHealthUseCase = viewPlatformBusinessHealthUseCase;
        this.viewGradingFailureOverviewUseCase = viewGradingFailureOverviewUseCase;
        this.viewGradingFailureSessionsUseCase = viewGradingFailureSessionsUseCase;
    }

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

    @QueryMapping(name = "teacherDashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherDashboardSummaryResponse teacherDashboard() {
        return viewTeacherDashboardUseCase.execute(null);
    }

    @QueryMapping(name = "nearestCentralizedExam")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public NearestCentralizedExamDto nearestCentralizedExam() {
        return viewNearestCentralizedExamUseCase.execute(null).orElse(null);
    }

    @QueryMapping(name = "questionBankStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public QuestionBankStatsDto questionBankStats() {
        return viewQuestionBankStatsUseCase.execute(null);
    }

    @QueryMapping(name = "platformOperationalHealth")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PlatformOperationalHealthResponse platformOperationalHealth(
            @Argument(name = "dateFrom") String dateFrom,
            @Argument(name = "dateTo") String dateTo) {
        return viewPlatformOperationalHealthUseCase.execute(new ViewPlatformOperationalHealthUseCase.Query(
            DateMapper.toInstant(dateFrom),
            DateMapper.toInstant(dateTo)
        ));
    }

    @QueryMapping(name = "platformBusinessHealth")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PlatformBusinessHealthResponse platformBusinessHealth(
            @Argument(name = "dateFrom") String dateFrom,
            @Argument(name = "dateTo") String dateTo) {
        return viewPlatformBusinessHealthUseCase.execute(new ViewPlatformBusinessHealthUseCase.Query(
            DateMapper.toInstant(dateFrom),
            DateMapper.toInstant(dateTo)
        ));
    }

    @QueryMapping(name = "gradingFailureOverview")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public GradingFailureOverviewResponse gradingFailureOverview(
            @Argument(name = "dateFrom") String dateFrom,
            @Argument(name = "dateTo") String dateTo) {
        return viewGradingFailureOverviewUseCase.execute(new ViewGradingFailureOverviewQuery(
            DateMapper.toInstant(dateFrom),
            DateMapper.toInstant(dateTo)
        ));
    }

    @QueryMapping(name = "gradingFailureSessions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<GradingFailureSessionDto> gradingFailureSessions(
            @Argument(name = "dateFrom") String dateFrom,
            @Argument(name = "dateTo") String dateTo,
            @Argument(name = "signature") String signature,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewGradingFailureSessionsUseCase.execute(new ViewGradingFailureSessionsQuery(
            DateMapper.toInstant(dateFrom),
            DateMapper.toInstant(dateTo),
            signature,
            page, 
            size
        ));
    }

}
