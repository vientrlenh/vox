package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewMyProctorScheduleCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewMyProctorSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewScheduleProctoringAlertsUseCase;
import com.sep.vox.application.projection.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.projection.dto.ProctorCandidateSummary;
import com.sep.vox.application.projection.dto.ProctorScheduleSummary;
import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;

@Controller("graphqlProctorAttendanceController")
public class ProctorAttendanceController {

    private final ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase;
    private final ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase;
    private final ViewScheduleProctoringAlertsUseCase viewScheduleProctoringAlertsUseCase;
    private final ViewMonitorableAssessmentsUseCase viewMonitorableAssessmentsUseCase;
    private final ViewMonitorableAssessmentUseCase viewMonitorableAssessmentUseCase;

    public ProctorAttendanceController(
            ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase,
            ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase,
            ViewScheduleProctoringAlertsUseCase viewScheduleProctoringAlertsUseCase,
            ViewMonitorableAssessmentsUseCase viewMonitorableAssessmentsUseCase,
            ViewMonitorableAssessmentUseCase viewMonitorableAssessmentUseCase) {
        this.viewMyProctorSchedulesUseCase = viewMyProctorSchedulesUseCase;
        this.viewMyProctorScheduleCandidatesUseCase = viewMyProctorScheduleCandidatesUseCase;
        this.viewScheduleProctoringAlertsUseCase = viewScheduleProctoringAlertsUseCase;
        this.viewMonitorableAssessmentsUseCase = viewMonitorableAssessmentsUseCase;
        this.viewMonitorableAssessmentUseCase = viewMonitorableAssessmentUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<ProctorScheduleSummary> myProctorSchedules() {
        return viewMyProctorSchedulesUseCase.execute(null);
    }

    /**
     * Kỳ thi đang diễn ra hoặc sắp diễn ra mà người đang đăng nhập giám sát được.
     *
     * <p>Cố ý KHÔNG dùng {@code assessments}: đó là đường vào của màn quản lý kỳ thi, và nới nó cho giám
     * thị đồng nghĩa mở luôn dashboard kỳ thi cho họ.
     */
    @QueryMapping(name = "monitorableAssessments")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<MonitoredAssessmentSummary> monitorableAssessments() {
        return viewMonitorableAssessmentsUseCase.execute(null);
    }

    /** Cùng lý do với {@code monitorableAssessments}, cho phần đầu trang danh sách ca thi. */
    @QueryMapping(name = "monitorableAssessment")
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
     * Lịch sử cảnh báo giám sát của cả ca thi, để màn giám sát dựng lại những gì đã xảy ra trước khi
     * người đang xem kết nối tới.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<AssessmentProctoringAlertDto> scheduleProctoringAlerts(
            @Argument(name = "scheduleId") UUID scheduleId) {
        return viewScheduleProctoringAlertsUseCase.execute(scheduleId);
    }
}
