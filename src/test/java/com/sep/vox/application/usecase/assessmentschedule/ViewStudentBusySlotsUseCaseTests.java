package com.sep.vox.application.usecase.assessmentschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewStudentBusySlotsQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.application.response.input.assessmentschedule.StudentBusySlotResponse;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

/**
 * The read API serving the candidate placement screen: who in the group is busy in the time window of which session.
 *
 * <p>Only a convenience layer for the UI, not a protection layer -- the real blocking rule lives in
 * {@code AssessmentScheduleCandidateConflictValidator}.
 */
class ViewStudentBusySlotsUseCaseTests {

    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private ViewStudentBusySlotsUseCase useCase;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID busyScheduleId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentDirectoryAccessService = mock(AssessmentDirectoryAccessService.class);
        useCase = new ViewStudentBusySlotsUseCase(
            assessmentScheduleRepository, assessmentCandidateRepository, assessmentDirectoryAccessService);

        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
    }

    @Test
    void should_return_empty_when_no_student_ids_given() {
        List<StudentBusySlotResponse> result = useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of()));

        assertThat(result).isEmpty();
        verify(assessmentScheduleRepository, never()).findById(any());
    }

    @Test
    void should_return_empty_when_no_schedule_ids_given() {
        List<StudentBusySlotResponse> result = useCase.execute(new ViewStudentBusySlotsQuery(List.of(), List.of(studentId)));

        assertThat(result).isEmpty();
        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    /** Học sinh đã ở trong chính ca đang xét thì không phải "bận" — họ đang ở đúng chỗ. */
    @Test
    void should_exclude_the_schedule_being_filled() {
        useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of(studentId)));

        verify(assessmentCandidateRepository)
            .findConflictsForStudents(anyCollection(), eq(start), eq(end), eq(scheduleId));
    }

    @Test
    void should_map_conflicts_to_iso_strings() {
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(studentId, busyScheduleId, start, end)));

        List<StudentBusySlotResponse> result = useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of(studentId)));

        assertThat(result).singleElement().satisfies(slot -> {
            assertThat(slot.studentId()).isEqualTo(studentId);
            assertThat(slot.targetScheduleId()).isEqualTo(scheduleId);
            assertThat(slot.busyScheduleId()).isEqualTo(busyScheduleId);
            assertThat(slot.startDate()).isEqualTo(start.toString());
            assertThat(slot.endDate()).isEqualTo(end.toString());
        });
    }

    @Test
    void should_ask_about_every_schedule_in_the_request() {
        UUID secondScheduleId = UUID.randomUUID();
        AssessmentSchedule second = new AssessmentSchedule();
        second.setId(secondScheduleId);
        second.setAssessmentId(assessmentId);
        second.setStatus(AssessmentScheduleStatus.DRAFT);
        second.setStartDate(start.plusSeconds(7200));
        second.setEndDate(end.plusSeconds(7200));
        when(assessmentScheduleRepository.findById(secondScheduleId)).thenReturn(Optional.of(second));

        useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId, secondScheduleId), List.of(studentId)));

        verify(assessmentCandidateRepository)
            .findConflictsForStudents(anyCollection(), eq(start), eq(end), eq(scheduleId));
        verify(assessmentCandidateRepository).findConflictsForStudents(
            anyCollection(), eq(start.plusSeconds(7200)), eq(end.plusSeconds(7200)), eq(secondScheduleId));
    }

    @Test
    void should_reject_when_caller_cannot_see_the_assessment_directory() {
        when(assessmentDirectoryAccessService.resolveByAssessmentId(assessmentId))
            .thenThrow(new ForbiddenException("Quyền truy cập bị từ chối"));

        assertThatThrownBy(() -> useCase.execute(
                new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of(studentId))))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    private AssessmentSchedule schedule() {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setStatus(AssessmentScheduleStatus.DRAFT);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        return schedule;
    }
}
