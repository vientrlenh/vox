package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.examcandidate.ViewMyProctorScheduleCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.ViewMyProctorSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableExamUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableExamsUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewScheduleProctoringAlertsUseCase;
import com.sep.vox.application.query.dto.MonitoredExamSummary;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.domain.dto.ExamProctoringAlertDto;

@Controller("graphqlProctorAttendanceController")
public class ProctorAttendanceController {

    private final ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase;
    private final ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase;
    private final ViewScheduleProctoringAlertsUseCase viewScheduleProctoringAlertsUseCase;
    private final ViewMonitorableExamsUseCase viewMonitorableExamsUseCase;
    private final ViewMonitorableExamUseCase viewMonitorableExamUseCase;

    public ProctorAttendanceController(
            ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase,
            ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase,
            ViewScheduleProctoringAlertsUseCase viewScheduleProctoringAlertsUseCase,
            ViewMonitorableExamsUseCase viewMonitorableExamsUseCase,
            ViewMonitorableExamUseCase viewMonitorableExamUseCase) {
        this.viewMyProctorSchedulesUseCase = viewMyProctorSchedulesUseCase;
        this.viewMyProctorScheduleCandidatesUseCase = viewMyProctorScheduleCandidatesUseCase;
        this.viewScheduleProctoringAlertsUseCase = viewScheduleProctoringAlertsUseCase;
        this.viewMonitorableExamsUseCase = viewMonitorableExamsUseCase;
        this.viewMonitorableExamUseCase = viewMonitorableExamUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<ProctorScheduleSummary> myProctorSchedules() {
        return viewMyProctorSchedulesUseCase.execute(null);
    }

    /**
     * Kỳ thi đang diễn ra hoặc sắp diễn ra mà người đang đăng nhập giám sát được.
     *
     * <p>Cố ý KHÔNG dùng {@code exams}: đó là đường vào của màn quản lý kỳ thi, và nới nó cho giám
     * thị đồng nghĩa mở luôn dashboard kỳ thi cho họ.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<MonitoredExamSummary> monitorableExams() {
        return viewMonitorableExamsUseCase.execute(null);
    }

    /** Cùng lý do với {@code monitorableExams}, cho phần đầu trang danh sách ca thi. */
    @QueryMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public MonitoredExamSummary monitorableExam(@Argument(name = "examId") UUID examId) {
        return viewMonitorableExamUseCase.execute(examId);
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
    public List<ExamProctoringAlertDto> scheduleProctoringAlerts(
            @Argument(name = "scheduleId") UUID scheduleId) {
        return viewScheduleProctoringAlertsUseCase.execute(scheduleId);
    }
}
