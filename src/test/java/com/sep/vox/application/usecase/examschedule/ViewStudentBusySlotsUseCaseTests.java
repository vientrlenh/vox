package com.sep.vox.application.usecase.examschedule;

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
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.examschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.ExamScheduleRepository;

/**
 * API đọc phục vụ màn xếp thí sinh: ai trong nhóm đang bận vào khung giờ của ca nào.
 *
 * <p>Chỉ là lớp tiện dụng cho giao diện, không phải lớp bảo vệ — luật chặn thật nằm ở
 * {@code ExamScheduleCandidateConflictValidator}.
 */
class ViewStudentBusySlotsUseCaseTests {

    private ExamScheduleRepository examScheduleRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamDirectoryAccessService examDirectoryAccessService;
    private ViewStudentBusySlotsUseCase useCase;

    private final UUID examId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID busyScheduleId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examDirectoryAccessService = mock(ExamDirectoryAccessService.class);
        useCase = new ViewStudentBusySlotsUseCase(
            examScheduleRepository, examCandidateRepository, examDirectoryAccessService);

        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
    }

    @Test
    void should_return_empty_when_no_student_ids_given() {
        var result = useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of()));

        assertThat(result).isEmpty();
        verify(examScheduleRepository, never()).findById(any());
    }

    @Test
    void should_return_empty_when_no_schedule_ids_given() {
        var result = useCase.execute(new ViewStudentBusySlotsQuery(List.of(), List.of(studentId)));

        assertThat(result).isEmpty();
        verify(examCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    /** Học sinh đã ở trong chính ca đang xét thì không phải "bận" — họ đang ở đúng chỗ. */
    @Test
    void should_exclude_the_schedule_being_filled() {
        useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of(studentId)));

        verify(examCandidateRepository)
            .findConflictsForStudents(anyCollection(), eq(start), eq(end), eq(scheduleId));
    }

    @Test
    void should_map_conflicts_to_iso_strings() {
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(studentId, busyScheduleId, start, end)));

        var result = useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of(studentId)));

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
        var secondScheduleId = UUID.randomUUID();
        var second = new ExamSchedule();
        second.setId(secondScheduleId);
        second.setExamId(examId);
        second.setStatus(ExamScheduleStatus.DRAFT);
        second.setStartDate(start.plusSeconds(7200));
        second.setEndDate(end.plusSeconds(7200));
        when(examScheduleRepository.findById(secondScheduleId)).thenReturn(Optional.of(second));

        useCase.execute(new ViewStudentBusySlotsQuery(List.of(scheduleId, secondScheduleId), List.of(studentId)));

        verify(examCandidateRepository)
            .findConflictsForStudents(anyCollection(), eq(start), eq(end), eq(scheduleId));
        verify(examCandidateRepository).findConflictsForStudents(
            anyCollection(), eq(start.plusSeconds(7200)), eq(end.plusSeconds(7200)), eq(secondScheduleId));
    }

    @Test
    void should_reject_when_caller_cannot_see_the_exam_directory() {
        when(examDirectoryAccessService.resolveByExamId(examId))
            .thenThrow(new ForbiddenException("Quyền truy cập bị từ chối"));

        assertThatThrownBy(() -> useCase.execute(
                new ViewStudentBusySlotsQuery(List.of(scheduleId), List.of(studentId))))
            .isInstanceOf(ForbiddenException.class);
        verify(examCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    private ExamSchedule schedule() {
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setStatus(ExamScheduleStatus.DRAFT);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        return schedule;
    }
}
