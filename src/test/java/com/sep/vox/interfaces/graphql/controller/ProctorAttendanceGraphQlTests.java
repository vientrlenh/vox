package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewMyProctorScheduleCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewMyProctorSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewMonitorableAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewScheduleProctoringAlertsUseCase;
import com.sep.vox.application.query.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends the proctoring summaries through the real schema and asserts the typed values: the {@code DateTime} scalar
 * and the schedule, candidate, session, assessment kind and assessment status enums.
 */
@GraphQlControllerTest(ProctorAttendanceController.class)
@MockitoBean(types = ViewScheduleProctoringAlertsUseCase.class)
class ProctorAttendanceGraphQlTests {

    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T03:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase;
    @MockitoBean
    private ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase;
    @MockitoBean
    private ViewMonitorableAssessmentsUseCase viewMonitorableAssessmentsUseCase;
    @MockitoBean
    private ViewMonitorableAssessmentUseCase viewMonitorableAssessmentUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_proctor_schedules_should_return_status_enum_and_date_time_values() {
        when(viewMyProctorSchedulesUseCase.execute(any())).thenReturn(List.of(new ProctorScheduleSummary(
            SCHEDULE_ID, ASSESSMENT_ID, "Midterm", null, null, START, END, AssessmentScheduleStatus.PUBLISHED)));

        graphQlTester.document("{ myProctorSchedules { status startDate endDate } }").execute()
            .errors().verify()
            .path("myProctorSchedules[0].status").entity(String.class).isEqualTo("PUBLISHED")
            .path("myProctorSchedules[0].startDate").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("myProctorSchedules[0].endDate").entity(String.class).isEqualTo("2026-08-01T03:00:00Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void monitorable_assessments_should_return_kind_and_status_enums_and_date_time_window() {
        when(viewMonitorableAssessmentsUseCase.execute(any())).thenReturn(List.of(new MonitoredAssessmentSummary(
            ASSESSMENT_ID, "AS-01", "Midterm", AssessmentKind.CENTRALIZED, AssessmentStatus.IN_PROGRESS, START, END, 2L)));

        graphQlTester.document("{ monitorableAssessments { kind status windowStart windowEnd liveScheduleCount } }").execute()
            .errors().verify()
            .path("monitorableAssessments[0].kind").entity(String.class).isEqualTo("CENTRALIZED")
            .path("monitorableAssessments[0].status").entity(String.class).isEqualTo("IN_PROGRESS")
            .path("monitorableAssessments[0].windowStart").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("monitorableAssessments[0].windowEnd").entity(String.class).isEqualTo("2026-08-01T03:00:00Z")
            .path("monitorableAssessments[0].liveScheduleCount").entity(Integer.class).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_proctor_schedule_candidates_should_return_status_enums_and_date_time_values() {
        when(viewMyProctorScheduleCandidatesUseCase.execute(any())).thenReturn(List.of(new ProctorCandidateSummary(
            UUID.randomUUID(), UUID.randomUUID(), "An", "an@example.com", AssessmentCandidateStatus.ATTENDED, null,
            UUID.randomUUID(), AssessmentSessionStatus.GRADING, false)));

        graphQlTester.document("{ myProctorScheduleCandidates(scheduleId: \"%s\") { status sessionStatus blockedAt } }"
            .formatted(SCHEDULE_ID)).execute()
            .errors().verify()
            .path("myProctorScheduleCandidates[0].status").entity(String.class).isEqualTo("ATTENDED")
            .path("myProctorScheduleCandidates[0].sessionStatus").entity(String.class).isEqualTo("GRADING")
            .path("myProctorScheduleCandidates[0].blockedAt").valueIsNull();
    }
}
