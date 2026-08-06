package com.sep.vox.interfaces.graphql.controller;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewTeacherDashboardUseCase;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto;
import com.sep.vox.domain.dto.SystemAdminDashboardSummaryDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto;

@Controller
public class DashboardController {

    private final ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase;
    private final ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase;
    private final ViewTeacherDashboardUseCase viewTeacherDashboardUseCase;

    public DashboardController(ViewSystemAdminDashboardUseCase viewSystemAdminDashboardUseCase,
            ViewSchoolAdminDashboardUseCase viewSchoolAdminDashboardUseCase,
            ViewTeacherDashboardUseCase viewTeacherDashboardUseCase) {
        this.viewSystemAdminDashboardUseCase = viewSystemAdminDashboardUseCase;
        this.viewSchoolAdminDashboardUseCase = viewSchoolAdminDashboardUseCase;
        this.viewTeacherDashboardUseCase = viewTeacherDashboardUseCase;
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

}
