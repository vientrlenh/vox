package com.sep.vox.interfaces.graphql.controller;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.dashboard.ViewNearestCentralizedExamUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewQuestionBankStatsUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewTeacherDashboardUseCase;
import com.sep.vox.application.query.dto.NearestCentralizedExamDto;
import com.sep.vox.application.query.dto.QuestionBankStatsDto;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto;
import com.sep.vox.domain.dto.SystemAdminDashboardSummaryDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto;

@Controller
public class DashboardController {

    private final ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase;
    private final ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase;
    private final ViewTeacherDashboardUseCase viewTeacherDashboardUseCase;
    private final ViewNearestCentralizedExamUseCase viewNearestCentralizedExamUseCase;
    private final ViewQuestionBankStatsUseCase viewQuestionBankStatsUseCase;

    public DashboardController(ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase,
            ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase,
            ViewTeacherDashboardUseCase viewTeacherDashboardUseCase,
            ViewNearestCentralizedExamUseCase viewNearestCentralizedExamUseCase,
            ViewQuestionBankStatsUseCase viewQuestionBankStatsUseCase) {
        this.viewSystemAdminDashboardUseCase = viewSystemAdminDashboardUseCase;
        this.viewSchoolAdminDashboardUseCase = viewSchoolAdminDashboardUseCase;
        this.viewTeacherDashboardUseCase = viewTeacherDashboardUseCase;
        this.viewNearestCentralizedExamUseCase = viewNearestCentralizedExamUseCase;
        this.viewQuestionBankStatsUseCase = viewQuestionBankStatsUseCase;
    }

    @QueryMapping(name = "systemAdminDashboard")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public SystemAdminDashboardSummaryDto systemAdminDashboard() {
        return viewSystemAdminDashboardUseCase.execute(null);
    }

    @QueryMapping(name = "schoolAdminDashboard")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolAdminDashboardSummaryDto schoolAdminDashboard() {
        return viewSchoolAdminDashboardUseCase.execute(null);
    }

    @QueryMapping(name = "teacherDashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherDashboardSummaryDto teacherDashboard() {
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

}
