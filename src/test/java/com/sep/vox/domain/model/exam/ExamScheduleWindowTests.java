package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class ExamScheduleWindowTests {

    private final OffsetDateTime start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00");

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
        var exam = examWithWindow(start, start.plusHours(5));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start.plusHours(1), start.plusHours(3))).isFalse();
    }

    @Test
    void should_accept_window_exactly_matching_exam_window() {
        var exam = examWithWindow(start, start.plusHours(5));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start, start.plusHours(5))).isFalse();
    }

    @Test
    void should_reject_window_starting_before_exam_open() {
        var exam = examWithWindow(start, start.plusHours(5));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start.minusSeconds(1), start.plusHours(3))).isTrue();
    }

    @Test
    void should_reject_window_ending_after_exam_close() {
        var exam = examWithWindow(start, start.plusHours(5));

        assertThat(exam.isScheduleWindowOutsideExamWindow(start.plusHours(1), start.plusHours(5).plusSeconds(1)))
            .isTrue();
    }

    @Test
    void should_skip_exam_window_check_when_bound_not_set() {
        // Kỳ thi thường được phép chưa set openAt/closeAt -- mỗi cận kiểm tra độc lập.
        assertThat(examWithWindow(null, null).isScheduleWindowOutsideExamWindow(start, start.plusHours(9))).isFalse();
        assertThat(examWithWindow(start, null).isScheduleWindowOutsideExamWindow(start, start.plusHours(9))).isFalse();
        assertThat(examWithWindow(null, start).isScheduleWindowOutsideExamWindow(start.minusHours(9), start)).isFalse();
    }

    @Test
    void should_skip_exam_window_check_when_schedule_window_incomplete() {
        var exam = examWithWindow(start, start.plusHours(5));

        assertThat(exam.isScheduleWindowOutsideExamWindow(null, start.plusHours(9))).isFalse();
        assertThat(exam.isScheduleWindowOutsideExamWindow(start.minusHours(9), null)).isFalse();
    }

    private Exam examWithDuration(Integer examTimeDurationSecond) {
        var exam = new Exam();
        exam.setExamTimeDurationSecond(examTimeDurationSecond);
        return exam;
    }

    private Exam examWithWindow(OffsetDateTime openAt, OffsetDateTime closeAt) {
        var exam = new Exam();
        exam.setOpenAt(openAt);
        exam.setCloseAt(closeAt);
        return exam;
    }
}
