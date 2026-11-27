package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewMyProctorScheduleCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewMyProctorSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewScheduleProctoringAlertsUseCase;
import com.sep.vox.application.query.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;

@Controller
@RequiredArgsConstructor
public class GraphQLProctorAttendanceController {

    private final ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase;
    private final ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase;
    private final ViewScheduleProctoringAlertsUseCase viewScheduleProctoringAlertsUseCase;
    private final ViewMonitorableAssessmentsUseCase viewMonitorableAssessmentsUseCase;
    private final ViewMonitorableAssessmentUseCase viewMonitorableAssessmentUseCase;

    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<ProctorScheduleSummary> myProctorSchedules() {
        return viewMyProctorSchedulesUseCase.execute(null);
    }

    /**
     * The assessments that are running or about to run that the logged-in user can proctor.
     *
     * <p>Deliberately does NOT use {@code assessments}: that is the entry to the assessment management screen, and loosening it for
     * proctors means opening the assessment dashboard to them as well.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<MonitoredAssessmentSummary> monitorableAssessments() {
        return viewMonitorableAssessmentsUseCase.execute(null);
    }

    /** The same reason as {@code monitorableAssessments}, for the top of the session list page. */
    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public MonitoredAssessmentSummary monitorableAssessment(@Argument(name = "assessmentId") UUID assessmentId) {
        return viewMonitorableAssessmentUseCase.execute(assessmentId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<ProctorCandidateSummary> myProctorScheduleCandidates(
            @Argument(name = "scheduleId") UUID scheduleId) {
        return viewMyProctorScheduleCandidatesUseCase.execute(scheduleId);
    }

    /**
     * Proctoring alert history of the whole schedule, so the monitoring screen can rebuild what happened before
     * the viewer connected.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<AssessmentProctoringAlertDto> scheduleProctoringAlerts(
            @Argument(name = "scheduleId") UUID scheduleId) {
        return viewScheduleProctoringAlertsUseCase.execute(scheduleId);
    }
}
