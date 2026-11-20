package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.query.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewMyAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.application.response.input.assessmentschedule.ProctorBusySlotResponse;
import com.sep.vox.application.response.input.assessmentschedule.StudentBusySlotResponse;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends schedule operations through the real schema and asserts the typed values: the {@code DateTime} scalar
 * (strict ISO-8601 with an offset) on schedules, busy slots and the update input, and the
 * {@code AssessmentScheduleStatus} enum.
 */
@GraphQlControllerTest(AssessmentScheduleController.class)
@MockitoBean(types = { AssessmentScheduleProctorRepository.class, AssessmentCandidateRepository.class,
    ViewMyAssessmentSchedulesUseCase.class })
class AssessmentScheduleGraphQlTests {

    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T03:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentSchedulesUseCase viewAssessmentSchedulesUseCase;
    @MockitoBean
    private UpdateAssessmentScheduleUseCase updateAssessmentScheduleUseCase;
    @MockitoBean
    private ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase;
    @MockitoBean
    private ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase;

    private static final String SCHEDULES_QUERY = """
        query($status: AssessmentScheduleStatus, $startDate: DateTime, $endDate: DateTime) {
            assessmentSchedules(status: $status, startDate: $startDate, endDate: $endDate) { status startDate endDate }
        }""";

    private static final String UPDATE_MUTATION = """
        mutation($startDate: DateTime, $endDate: DateTime) {
            updateAssessmentSchedule(id: "%s", input: { startDate: $startDate, endDate: $endDate })
        }""".formatted(SCHEDULE_ID);

    private static AssessmentScheduleDto schedule() {
        return new AssessmentScheduleDto(SCHEDULE_ID, ASSESSMENT_ID, null, START, END, AssessmentScheduleStatus.PUBLISHED, null);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void schedules_should_return_status_enum_and_date_time_values() {
        when(viewAssessmentSchedulesUseCase.execute(any())).thenReturn(List.of(schedule()));

        graphQlTester.document(SCHEDULES_QUERY).execute()
            .errors().verify()
            .path("assessmentSchedules[0].status").entity(String.class).isEqualTo("PUBLISHED")
            .path("assessmentSchedules[0].startDate").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessmentSchedules[0].endDate").entity(String.class).isEqualTo("2026-08-01T03:00:00Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void schedules_should_bind_status_and_offset_date_time_variables() {
        when(viewAssessmentSchedulesUseCase.execute(any())).thenReturn(List.of());

        graphQlTester.document(SCHEDULES_QUERY).variable("status", "DRAFT")
            .variable("startDate", "2026-08-01T08:00:00+07:00").variable("endDate", "2026-08-01T10:00:00+07:00").execute()
            .errors().verify();

        verify(viewAssessmentSchedulesUseCase).execute(new ViewAssessmentSchedulesQuery(null, AssessmentScheduleStatus.DRAFT, START, END));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void schedules_should_reject_a_date_without_an_offset() {
        graphQlTester.document(SCHEDULES_QUERY).variable("startDate", "2026-08-01").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("startDate"))
            .verify();

        verifyNoInteractions(viewAssessmentSchedulesUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void schedules_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ assessmentSchedules(status: NOT_A_STATUS) { id } }").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewAssessmentSchedulesUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_schedule_should_deliver_instants_to_the_command() {
        when(updateAssessmentScheduleUseCase.execute(any())).thenReturn(SCHEDULE_ID);

        graphQlTester.document(UPDATE_MUTATION)
            .variable("startDate", "2026-08-01T08:00:00+07:00").variable("endDate", "2026-08-01T10:00:00+07:00").execute()
            .errors().verify();

        verify(updateAssessmentScheduleUseCase).execute(new UpdateAssessmentScheduleCommand(SCHEDULE_ID, null, START, END));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_schedule_should_reject_a_date_without_an_offset() {
        graphQlTester.document(UPDATE_MUTATION).variable("startDate", "2026-08-01T08:00:00").variable("endDate", null).execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("startDate"))
            .verify();

        verifyNoInteractions(updateAssessmentScheduleUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void busy_slots_should_return_date_time_values() {
        UUID teacherId = UUID.randomUUID();
        when(viewProctorBusySlotsUseCase.execute(any())).thenReturn(List.of(
            new ProctorBusySlotResponse(teacherId, SCHEDULE_ID, START, END)));
        when(viewStudentBusySlotsUseCase.execute(any())).thenReturn(List.of(
            new StudentBusySlotResponse(teacherId, SCHEDULE_ID, UUID.randomUUID(), START, null)));

        graphQlTester.document("""
            { proctorBusySlots(scheduleId: "%1$s", teacherIds: ["%2$s"]) { startDate endDate }
              studentBusySlots(scheduleIds: ["%1$s"], studentIds: ["%2$s"]) { startDate endDate } }"""
            .formatted(SCHEDULE_ID, teacherId)).execute()
            .errors().verify()
            .path("proctorBusySlots[0].startDate").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("proctorBusySlots[0].endDate").entity(String.class).isEqualTo("2026-08-01T03:00:00Z")
            .path("studentBusySlots[0].startDate").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("studentBusySlots[0].endDate").valueIsNull();
    }
}
