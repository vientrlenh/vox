package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class ExamScheduleWindowTests {

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @Test
    void should_accept_window_longer_than_exam_time() {
        var exam = examWithDuration(3600);

        assertThat(exam.isScheduleWindowShorterThanExamTime(start, start.plusSeconds(3601))).isFalse();
    }

    @Test
    void should_accept_window_equal_to_exam_time() {
        var exam = examWithDuration(3600);

        assertThat(exam.isScheduleWindowShorterThanExamTime(start, start.plusSeconds(3600))).isFalse();
    }

    @Test
    void should_reject_window_shorter_than_exam_time() {
        var exam = examWithDuration(3600);

        assertThat(exam.isScheduleWindowShorterThanExamTime(start, start.plusSeconds(3599))).isTrue();
    }

    @Test
    void should_skip_check_when_exam_time_not_calculated_yet() {
        assertThat(examWithDuration(null).isScheduleWindowShorterThanExamTime(start, start.plusSeconds(1))).isFalse();
        assertThat(examWithDuration(0).isScheduleWindowShorterThanExamTime(start, start.plusSeconds(1))).isFalse();
    }

    @Test
    void should_skip_check_when_window_incomplete() {
        var exam = examWithDuration(3600);

        assertThat(exam.isScheduleWindowShorterThanExamTime(null, start.plusSeconds(1))).isFalse();
        assertThat(exam.isScheduleWindowShorterThanExamTime(start, null)).isFalse();
    }

    @Test
    void should_accept_window_inside_exam_window() {
        var exam = examWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start.plus(1, ChronoUnit.HOURS), start.plus(3, ChronoUnit.HOURS))).isFalse();
    }

    @Test
    void should_accept_window_exactly_matching_exam_window() {
        var exam = examWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start, start.plus(5, ChronoUnit.HOURS))).isFalse();
    }

    @Test
    void should_reject_window_starting_before_exam_open() {
        var exam = examWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start.minusSeconds(1), start.plus(3, ChronoUnit.HOURS))).isTrue();
    }

    @Test
    void should_reject_window_ending_after_exam_close() {
        var exam = examWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start.plus(1, ChronoUnit.HOURS), start.plus(5, ChronoUnit.HOURS).plusSeconds(1)))
            .isTrue();
    }

    @Test
    void should_skip_exam_window_check_when_bound_not_set() {
        // Kỳ thi thường được phép chưa set openAt/closeAt -- mỗi cận kiểm tra độc lập.
        assertThat(examWithWindow(null, null).isScheduleWindowOutsideExamWindow(start, start.plus(9, ChronoUnit.HOURS))).isFalse();
        assertThat(examWithWindow(start, null).isScheduleWindowOutsideExamWindow(start, start.plus(9, ChronoUnit.HOURS))).isFalse();
        assertThat(examWithWindow(null, start).isScheduleWindowOutsideExamWindow(start.minus(9, ChronoUnit.HOURS), start)).isFalse();
    }

    @Test
    void should_skip_exam_window_check_when_schedule_window_incomplete() {
        var exam = examWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(exam.isScheduleWindowOutsideExamWindow(null, start.plus(9, ChronoUnit.HOURS))).isFalse();
        assertThat(exam.isScheduleWindowOutsideExamWindow(start.minus(9, ChronoUnit.HOURS), null)).isFalse();
    }

    private Exam examWithDuration(Integer examTimeDurationSecond) {
        var exam = new Exam();
        exam.setExamTimeDurationSecond(examTimeDurationSecond);
        return exam;
    }

    private Exam examWithWindow(Instant openAt, Instant closeAt) {
        var exam = new Exam();
        exam.setOpenAt(openAt);
        exam.setCloseAt(closeAt);
        return exam;
    }
}
