package com.sep.vox.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
 * A thin job: runs the real service on a fake repo so the wiring is caught too (batch cap, not touching schedules
 * that have not ended) and not only a check that it was called.
 */
class AssessmentScheduleAutoCompleteJobTests {

    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleAutoCompleteJob job;

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        job = new AssessmentScheduleAutoCompleteJob(new AssessmentScheduleClosureService(
            assessmentScheduleRepository, mock(AssessmentSessionRepository.class)));
    }

    @Test
    void should_complete_schedules_returned_by_the_sweep() {
        AssessmentSchedule ended = schedule(start, start.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findPublishedEndedBefore(any(), eq(200))).thenReturn(List.of(ended));

        job.run();

        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        verify(assessmentScheduleRepository).save(ended);
    }

    @Test
    void should_do_nothing_when_no_schedule_has_ended() {
        when(assessmentScheduleRepository.findPublishedEndedBefore(any(), eq(200))).thenReturn(List.of());

        job.run();

        verify(assessmentScheduleRepository, never()).save(any());
    }

    private AssessmentSchedule schedule(Instant from, Instant to) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(UUID.randomUUID());
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(AssessmentScheduleStatus.PUBLISHED);
        return schedule;
    }
}
