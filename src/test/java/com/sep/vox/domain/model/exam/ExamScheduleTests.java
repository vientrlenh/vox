package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

/**
 * Vị từ thời gian của ca thi. Đặt ở domain để "ca đang diễn ra / đã hết giờ" chỉ được phát biểu một
 * chỗ — service đóng ca, worker quét ca và test đều đọc chung một định nghĩa.
 */
class ExamScheduleTests {

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();
    private final Instant end = start.plus(2, ChronoUnit.HOURS);

    @Test
    void should_treat_start_boundary_as_already_started() {
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, start, end);

        assertThat(schedule.hasStartedAt(start.minusSeconds(1))).isFalse();
        assertThat(schedule.hasStartedAt(start)).isTrue();
    }

    /**
     * Biên phải khớp với {@code findByIdAndInSchedule} (WHERE endDate > :now): đúng khoảnh khắc
     * endDate là ca đã hết giờ, không còn vào thi được.
     */
    @Test
    void should_treat_end_boundary_as_already_ended() {
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, start, end);

        assertThat(schedule.hasEndedAt(end.minusSeconds(1))).isFalse();
        assertThat(schedule.hasEndedAt(end)).isTrue();
    }

    @Test
    void should_report_ongoing_only_within_window() {
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, start, end);

        assertThat(schedule.isOngoingAt(start.minusSeconds(1))).isFalse();
        assertThat(schedule.isOngoingAt(start)).isTrue();
        assertThat(schedule.isOngoingAt(end.minusSeconds(1))).isTrue();
        assertThat(schedule.isOngoingAt(end)).isFalse();
    }

    /** Chỉ ca đã công bố mới "đang diễn ra" — ca nháp chưa ai nhìn thấy để mà vào thi. */
    @Test
    void should_not_report_ongoing_for_non_published_status() {
        var midway = start.plus(1, ChronoUnit.HOURS);

        for (var status : ExamScheduleStatus.values()) {
            var schedule = schedule(status, start, end);
            assertThat(schedule.isOngoingAt(midway))
                .as("status %s", status)
                .isEqualTo(status == ExamScheduleStatus.PUBLISHED);
        }
    }

    @Test
    void should_not_report_started_or_ended_when_dates_missing() {
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, null, null);

        assertThat(schedule.hasStartedAt(start)).isFalse();
        assertThat(schedule.hasEndedAt(start)).isFalse();
        assertThat(schedule.isOngoingAt(start)).isFalse();
    }

    private ExamSchedule schedule(ExamScheduleStatus status, Instant from, Instant to) {
        var schedule = new ExamSchedule();
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
