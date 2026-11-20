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

import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Job mỏng: chạy service thật trên repo giả để bắt được cả phần nối dây (trần lô, không đụng ca
 * chưa hết giờ) chứ không chỉ xác nhận có gọi.
 */
class ExamScheduleAutoCompleteJobTests {

    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleAutoCompleteJob job;

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        examScheduleRepository = mock(ExamScheduleRepository.class);
        job = new ExamScheduleAutoCompleteJob(new ExamScheduleClosureService(
            examScheduleRepository, mock(ExamSessionRepository.class)));
    }

    @Test
    void should_complete_schedules_returned_by_the_sweep() {
        var ended = schedule(start, start.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findPublishedEndedBefore(any(), eq(200))).thenReturn(List.of(ended));

        job.run();

        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        verify(examScheduleRepository).save(ended);
    }

    @Test
    void should_do_nothing_when_no_schedule_has_ended() {
        when(examScheduleRepository.findPublishedEndedBefore(any(), eq(200))).thenReturn(List.of());

        job.run();

        verify(examScheduleRepository, never()).save(any());
    }

    private ExamSchedule schedule(Instant from, Instant to) {
        var schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(UUID.randomUUID());
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(ExamScheduleStatus.PUBLISHED);
        return schedule;
    }
}
