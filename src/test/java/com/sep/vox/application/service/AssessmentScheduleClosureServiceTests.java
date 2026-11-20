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

import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Luật đóng ca thi được phát biểu ở đúng một chỗ. Ba đường gọi vào đây — đóng bài, huỷ/xoá bài, và
 * job quét ca hết giờ — nên bảng ánh xạ trạng thái được test đầy đủ tại lớp này, còn test của use
 * case chỉ xác nhận có gọi đúng.
 */
class AssessmentScheduleClosureServiceTests {

    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentScheduleClosureService service;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();
    private final Instant end = start.plus(2, ChronoUnit.HOURS);
    /** Mốc "bây giờ" nằm sau khi ca đã kết thúc. */
    private final Instant afterEnd = end.plus(5, ChronoUnit.MINUTES);

    @BeforeEach
    void setUp() {
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        service = new AssessmentScheduleClosureService(assessmentScheduleRepository, assessmentSessionRepository);
    }

    // ---------- guard: chặn đóng bài khi còn người đang làm bài ----------

    @Test
    void should_reject_close_when_ongoing_schedule_still_has_active_session() {
        AssessmentSchedule ongoing = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        when(assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, start)).thenReturn(List.of(ongoing));
        when(assessmentSessionRepository.countActiveByAssessmentId(assessmentId)).thenReturn(3L);

        assertThatThrownBy(() -> service.requireNoActiveSessionInOngoingSchedule(assessmentId, start))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("3 học sinh đang làm bài");
    }

    /**
     * Even when the whole class has already submitted, the teacher must still be able to close
     * early -- this is why there is no hard block by time.
     */
    @Test
    void should_allow_close_when_ongoing_schedule_has_no_active_session() {
        AssessmentSchedule ongoing = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        when(assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, start)).thenReturn(List.of(ongoing));
        when(assessmentSessionRepository.countActiveByAssessmentId(assessmentId)).thenReturn(0L);

        service.requireNoActiveSessionInOngoingSchedule(assessmentId, start);
    }

    @Test
    void should_not_count_sessions_when_no_schedule_is_ongoing() {
        when(assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, afterEnd)).thenReturn(List.of());

        service.requireNoActiveSessionInOngoingSchedule(assessmentId, afterEnd);

        verifyNoInteractions(assessmentSessionRepository);
    }

    // ---------- cascade khi ĐÓNG bài ----------

    @Test
    void should_complete_published_schedule_that_already_ended() {
        AssessmentSchedule ended = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(ended));

        int changed = service.closeSchedulesForAssessment(assessmentId, actorId, afterEnd);

        assertThat(changed).isEqualTo(1);
        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(ended.getUpdatedAt()).isEqualTo(afterEnd);
        assertThat(ended.getUpdatedBy()).isEqualTo(actorId);
        verify(assessmentScheduleRepository).save(ended);
    }

    /**
     * A session that has not reached its time will never take place -- calling it "completed" would
     * falsify history.
     */
    @Test
    void should_cancel_published_schedule_that_never_started() {
        AssessmentSchedule notStarted = schedule(AssessmentScheduleStatus.PUBLISHED, afterEnd, afterEnd.plus(1, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(notStarted));

        service.closeSchedulesForAssessment(assessmentId, actorId, start);

        assertThat(notStarted.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        verify(assessmentScheduleRepository).save(notStarted);
    }

    @Test
    void should_cancel_draft_schedule_on_close() {
        AssessmentSchedule draft = schedule(AssessmentScheduleStatus.DRAFT, start, end);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(draft));

        service.closeSchedulesForAssessment(assessmentId, actorId, afterEnd);

        assertThat(draft.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
    }

    /**
     * COMPLETED/MOVED/CANCELLED are final states: overwriting them would erase the trace of a
     * session that was already held and skew {@code movedToScheduleId}. DELETED is already filtered
     * out by {@code findByAssessmentId}.
     */
    @Test
    void should_not_overwrite_terminal_schedule_status_on_close() {
        AssessmentSchedule completed = schedule(AssessmentScheduleStatus.COMPLETED, start, end);
        AssessmentSchedule moved = schedule(AssessmentScheduleStatus.MOVED, start, end);
        AssessmentSchedule cancelled = schedule(AssessmentScheduleStatus.CANCELLED, start, end);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(completed, moved, cancelled));

        int changed = service.closeSchedulesForAssessment(assessmentId, actorId, afterEnd);

        assertThat(changed).isZero();
        assertThat(completed.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(moved.getStatus()).isEqualTo(AssessmentScheduleStatus.MOVED);
        assertThat(cancelled.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * The automatic close job has no user: writing null over it would erase whoever edited last.
     */
    @Test
    void should_keep_previous_updated_by_when_actor_is_null() {
        UUID previousActor = UUID.randomUUID();
        AssessmentSchedule ended = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        ended.setUpdatedBy(previousActor);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(ended));

        service.closeSchedulesForAssessment(assessmentId, null, afterEnd);

        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(ended.getUpdatedBy()).isEqualTo(previousActor);
    }

    // ---------- cascade khi HUỶ/XOÁ bài ----------

    @Test
    void should_cancel_draft_and_published_schedules_on_cancel() {
        AssessmentSchedule draft = schedule(AssessmentScheduleStatus.DRAFT, start, end);
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(draft, published));

        int changed = service.cancelSchedulesForAssessment(assessmentId, actorId, afterEnd);

        assertThat(changed).isEqualTo(2);
        assertThat(draft.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        assertThat(published.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        assertThat(published.getUpdatedBy()).isEqualTo(actorId);
        verify(assessmentScheduleRepository).save(draft);
        verify(assessmentScheduleRepository).save(published);
    }

    /**
     * Cancelling an assessment makes no distinction between sessions that have run and those that have
     * not -- completely unlike closing it.
     */
    @Test
    void should_cancel_even_a_schedule_that_already_ended() {
        AssessmentSchedule ended = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(ended));

        service.cancelSchedulesForAssessment(assessmentId, actorId, afterEnd);

        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
    }

    @Test
    void should_not_overwrite_terminal_schedule_status_on_cancel() {
        AssessmentSchedule completed = schedule(AssessmentScheduleStatus.COMPLETED, start, end);
        AssessmentSchedule moved = schedule(AssessmentScheduleStatus.MOVED, start, end);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(completed, moved));

        assertThat(service.cancelSchedulesForAssessment(assessmentId, actorId, afterEnd)).isZero();
        verify(assessmentScheduleRepository, never()).save(any());
    }

    // ---------- job quét ca đã hết giờ ----------

    @Test
    void should_complete_ended_schedules_found_by_sweep() {
        AssessmentSchedule ended = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);
        when(assessmentScheduleRepository.findPublishedEndedBefore(afterEnd, 200)).thenReturn(List.of(ended));

        int changed = service.completeEndedSchedules(afterEnd, 200);

        assertThat(changed).isEqualTo(1);
        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(ended.getUpdatedAt()).isEqualTo(afterEnd);
        assertThat(ended.getUpdatedBy()).isNull();
        verify(assessmentScheduleRepository).save(ended);
    }

    @Test
    void should_do_nothing_when_sweep_finds_no_ended_schedule() {
        when(assessmentScheduleRepository.findPublishedEndedBefore(afterEnd, 200)).thenReturn(List.of());

        assertThat(service.completeEndedSchedules(afterEnd, 200)).isZero();
        verify(assessmentScheduleRepository, never()).save(any());
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status, Instant from, Instant to) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(assessmentId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
