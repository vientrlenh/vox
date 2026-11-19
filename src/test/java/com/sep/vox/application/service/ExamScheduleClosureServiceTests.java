package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Luật đóng ca thi được phát biểu ở đúng một chỗ. Ba đường gọi vào đây — đóng bài, huỷ/xoá bài, và
 * job quét ca hết giờ — nên bảng ánh xạ trạng thái được test đầy đủ tại lớp này, còn test của use
 * case chỉ xác nhận có gọi đúng.
 */
class ExamScheduleClosureServiceTests {

    private ExamScheduleRepository examScheduleRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamScheduleClosureService service;

    private final UUID examId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();
    private final Instant end = start.plus(2, ChronoUnit.HOURS);
    /** Mốc "bây giờ" nằm sau khi ca đã kết thúc. */
    private final Instant afterEnd = end.plus(5, ChronoUnit.MINUTES);

    @BeforeEach
    void setUp() {
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        service = new ExamScheduleClosureService(examScheduleRepository, examSessionRepository);
    }

    // ---------- guard: chặn đóng bài khi còn người đang làm bài ----------

    @Test
    void should_reject_close_when_ongoing_schedule_still_has_active_session() {
        ExamSchedule ongoing = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        when(examScheduleRepository.findByExamIdAndInSchedule(examId, start)).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(3L);

        assertThatThrownBy(() -> service.requireNoActiveSessionInOngoingSchedule(examId, start))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("3 học sinh đang làm bài");
    }

    /**
     * Even when the whole class has already submitted, the teacher must still be able to close
     * early -- this is why there is no hard block by time.
     */
    @Test
    void should_allow_close_when_ongoing_schedule_has_no_active_session() {
        ExamSchedule ongoing = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        when(examScheduleRepository.findByExamIdAndInSchedule(examId, start)).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(0L);

        service.requireNoActiveSessionInOngoingSchedule(examId, start);
    }

    @Test
    void should_not_count_sessions_when_no_schedule_is_ongoing() {
        when(examScheduleRepository.findByExamIdAndInSchedule(examId, afterEnd)).thenReturn(List.of());

        service.requireNoActiveSessionInOngoingSchedule(examId, afterEnd);

        verifyNoInteractions(examSessionRepository);
    }

    // ---------- cascade khi ĐÓNG bài ----------

    @Test
    void should_complete_published_schedule_that_already_ended() {
        ExamSchedule ended = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ended));

        int changed = service.closeSchedulesForExam(examId, actorId, afterEnd);

        assertThat(changed).isEqualTo(1);
        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(ended.getUpdatedAt()).isEqualTo(afterEnd);
        assertThat(ended.getUpdatedBy()).isEqualTo(actorId);
        verify(examScheduleRepository).save(ended);
    }

    /**
     * A session that has not reached its time will never take place -- calling it "completed" would
     * falsify history.
     */
    @Test
    void should_cancel_published_schedule_that_never_started() {
        ExamSchedule notStarted = schedule(ExamScheduleStatus.PUBLISHED, afterEnd, afterEnd.plus(1, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(notStarted));

        service.closeSchedulesForExam(examId, actorId, start);

        assertThat(notStarted.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        verify(examScheduleRepository).save(notStarted);
    }

    @Test
    void should_cancel_draft_schedule_on_close() {
        ExamSchedule draft = schedule(ExamScheduleStatus.DRAFT, start, end);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(draft));

        service.closeSchedulesForExam(examId, actorId, afterEnd);

        assertThat(draft.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
    }

    /**
     * COMPLETED/MOVED/CANCELLED are final states: overwriting them would erase the trace of a
     * session that was already held and skew {@code movedToScheduleId}. DELETED is already filtered
     * out by {@code findByExamId}.
     */
    @Test
    void should_not_overwrite_terminal_schedule_status_on_close() {
        ExamSchedule completed = schedule(ExamScheduleStatus.COMPLETED, start, end);
        ExamSchedule moved = schedule(ExamScheduleStatus.MOVED, start, end);
        ExamSchedule cancelled = schedule(ExamScheduleStatus.CANCELLED, start, end);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(completed, moved, cancelled));

        int changed = service.closeSchedulesForExam(examId, actorId, afterEnd);

        assertThat(changed).isZero();
        assertThat(completed.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(moved.getStatus()).isEqualTo(ExamScheduleStatus.MOVED);
        assertThat(cancelled.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        verify(examScheduleRepository, never()).save(any());
    }

    /**
     * The automatic close job has no user: writing null over it would erase whoever edited last.
     */
    @Test
    void should_keep_previous_updated_by_when_actor_is_null() {
        UUID previousActor = UUID.randomUUID();
        ExamSchedule ended = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        ended.setUpdatedBy(previousActor);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ended));

        service.closeSchedulesForExam(examId, null, afterEnd);

        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(ended.getUpdatedBy()).isEqualTo(previousActor);
    }

    // ---------- cascade khi HUỶ/XOÁ bài ----------

    @Test
    void should_cancel_draft_and_published_schedules_on_cancel() {
        ExamSchedule draft = schedule(ExamScheduleStatus.DRAFT, start, end);
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(draft, published));

        int changed = service.cancelSchedulesForExam(examId, actorId, afterEnd);

        assertThat(changed).isEqualTo(2);
        assertThat(draft.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        assertThat(published.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        assertThat(published.getUpdatedBy()).isEqualTo(actorId);
        verify(examScheduleRepository).save(draft);
        verify(examScheduleRepository).save(published);
    }

    /**
     * Cancelling an exam makes no distinction between sessions that have run and those that have
     * not -- completely unlike closing it.
     */
    @Test
    void should_cancel_even_a_schedule_that_already_ended() {
        ExamSchedule ended = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ended));

        service.cancelSchedulesForExam(examId, actorId, afterEnd);

        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
    }

    @Test
    void should_not_overwrite_terminal_schedule_status_on_cancel() {
        ExamSchedule completed = schedule(ExamScheduleStatus.COMPLETED, start, end);
        ExamSchedule moved = schedule(ExamScheduleStatus.MOVED, start, end);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(completed, moved));

        assertThat(service.cancelSchedulesForExam(examId, actorId, afterEnd)).isZero();
        verify(examScheduleRepository, never()).save(any());
    }

    // ---------- job quét ca đã hết giờ ----------

    @Test
    void should_complete_ended_schedules_found_by_sweep() {
        ExamSchedule ended = schedule(ExamScheduleStatus.PUBLISHED, start, end);
        when(examScheduleRepository.findPublishedEndedBefore(afterEnd, 200)).thenReturn(List.of(ended));

        int changed = service.completeEndedSchedules(afterEnd, 200);

        assertThat(changed).isEqualTo(1);
        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(ended.getUpdatedAt()).isEqualTo(afterEnd);
        assertThat(ended.getUpdatedBy()).isNull();
        verify(examScheduleRepository).save(ended);
    }

    @Test
    void should_do_nothing_when_sweep_finds_no_ended_schedule() {
        when(examScheduleRepository.findPublishedEndedBefore(afterEnd, 200)).thenReturn(List.of());

        assertThat(service.completeEndedSchedules(afterEnd, 200)).isZero();
        verify(examScheduleRepository, never()).save(any());
    }

    private ExamSchedule schedule(ExamScheduleStatus status, Instant from, Instant to) {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(examId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
