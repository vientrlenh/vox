package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class AssessmentScheduleWindowTests {

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @Test
    void should_accept_window_longer_than_assessment_time() {
        Assessment assessment = assessmentWithDuration(3600);

        assertThat(assessment.isScheduleWindowShorterThanAssessmentTime(start, start.plusSeconds(3601))).isFalse();
    }

    @Test
    void should_accept_window_equal_to_assessment_time() {
        Assessment assessment = assessmentWithDuration(3600);

        assertThat(assessment.isScheduleWindowShorterThanAssessmentTime(start, start.plusSeconds(3600))).isFalse();
    }

    @Test
    void should_reject_window_shorter_than_assessment_time() {
        Assessment assessment = assessmentWithDuration(3600);

        assertThat(assessment.isScheduleWindowShorterThanAssessmentTime(start, start.plusSeconds(3599))).isTrue();
    }

    @Test
    void should_skip_check_when_assessment_time_not_calculated_yet() {
        assertThat(assessmentWithDuration(null).isScheduleWindowShorterThanAssessmentTime(start, start.plusSeconds(1))).isFalse();
        assertThat(assessmentWithDuration(0).isScheduleWindowShorterThanAssessmentTime(start, start.plusSeconds(1))).isFalse();
    }

    @Test
    void should_skip_check_when_window_incomplete() {
        Assessment assessment = assessmentWithDuration(3600);

        assertThat(assessment.isScheduleWindowShorterThanAssessmentTime(null, start.plusSeconds(1))).isFalse();
        assertThat(assessment.isScheduleWindowShorterThanAssessmentTime(start, null)).isFalse();
    }

    @Test
    void should_accept_window_inside_assessment_window() {
        Assessment assessment = assessmentWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(assessment.isScheduleWindowOutsideAssessmentWindow(start.plus(1, ChronoUnit.HOURS), start.plus(3, ChronoUnit.HOURS))).isFalse();
    }

    @Test
    void should_accept_window_exactly_matching_assessment_window() {
        Assessment assessment = assessmentWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(assessment.isScheduleWindowOutsideAssessmentWindow(start, start.plus(5, ChronoUnit.HOURS))).isFalse();
    }

    @Test
    void should_reject_window_starting_before_assessment_open() {
        Assessment assessment = assessmentWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(assessment.isScheduleWindowOutsideAssessmentWindow(start.minusSeconds(1), start.plus(3, ChronoUnit.HOURS))).isTrue();
    }

    @Test
    void should_reject_window_ending_after_assessment_close() {
        Assessment assessment = assessmentWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(assessment.isScheduleWindowOutsideAssessmentWindow(start.plus(1, ChronoUnit.HOURS), start.plus(5, ChronoUnit.HOURS).plusSeconds(1)))
            .isTrue();
    }

    @Test
    void should_skip_assessment_window_check_when_bound_not_set() {
        // Kỳ thi thường được phép chưa set openAt/closeAt -- mỗi cận kiểm tra độc lập.
        assertThat(assessmentWithWindow(null, null).isScheduleWindowOutsideAssessmentWindow(start, start.plus(9, ChronoUnit.HOURS))).isFalse();
        assertThat(assessmentWithWindow(start, null).isScheduleWindowOutsideAssessmentWindow(start, start.plus(9, ChronoUnit.HOURS))).isFalse();
        assertThat(assessmentWithWindow(null, start).isScheduleWindowOutsideAssessmentWindow(start.minus(9, ChronoUnit.HOURS), start)).isFalse();
    }

    @Test
    void should_skip_assessment_window_check_when_schedule_window_incomplete() {
        Assessment assessment = assessmentWithWindow(start, start.plus(5, ChronoUnit.HOURS));

        assertThat(assessment.isScheduleWindowOutsideAssessmentWindow(null, start.plus(9, ChronoUnit.HOURS))).isFalse();
        assertThat(assessment.isScheduleWindowOutsideAssessmentWindow(start.minus(9, ChronoUnit.HOURS), null)).isFalse();
    }

    private Assessment assessmentWithDuration(Integer assessmentTimeDurationSecond) {
        Assessment assessment = new Assessment();
        assessment.setAssessmentTimeDurationSecond(assessmentTimeDurationSecond);
        return assessment;
    }

    private Assessment assessmentWithWindow(Instant openAt, Instant closeAt) {
        Assessment assessment = new Assessment();
        assessment.setOpenAt(openAt);
        assessment.setCloseAt(closeAt);
        return assessment;
    }
}
