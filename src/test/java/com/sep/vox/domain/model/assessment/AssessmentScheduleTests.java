package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

/**
 * Time predicates of a schedule. Placed in domain so that "schedule in progress / time is up" is stated in only one
 * place — the schedule-closing service, the schedule-scanning worker and the tests all read the same definition.
 */
class AssessmentScheduleTests {

    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();
    private final Instant end = start.plus(2, ChronoUnit.HOURS);

    @Test
    void should_treat_start_boundary_as_already_started() {
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);

        assertThat(schedule.hasStartedAt(start.minusSeconds(1))).isFalse();
        assertThat(schedule.hasStartedAt(start)).isTrue();
    }

    /**
     * The boundary must match {@code findByIdAndInSchedule} (WHERE endDate > :now): at the exact
     * moment of endDate the schedule has ended and the candidate can no longer enter the assessment.
     */
    @Test
    void should_treat_end_boundary_as_already_ended() {
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);

        assertThat(schedule.hasEndedAt(end.minusSeconds(1))).isFalse();
        assertThat(schedule.hasEndedAt(end)).isTrue();
    }

    @Test
    void should_report_ongoing_only_within_window() {
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, start, end);

        assertThat(schedule.isOngoingAt(start.minusSeconds(1))).isFalse();
        assertThat(schedule.isOngoingAt(start)).isTrue();
        assertThat(schedule.isOngoingAt(end.minusSeconds(1))).isTrue();
        assertThat(schedule.isOngoingAt(end)).isFalse();
    }

    /**
     * Only a published schedule is "in progress" -- nobody can see a draft schedule, so nobody can
     * enter it.
     */
    @Test
    void should_not_report_ongoing_for_non_published_status() {
        Instant midway = start.plus(1, ChronoUnit.HOURS);

        for (AssessmentScheduleStatus status : AssessmentScheduleStatus.values()) {
            AssessmentSchedule schedule = schedule(status, start, end);
            assertThat(schedule.isOngoingAt(midway))
                .as("status %s", status)
                .isEqualTo(status == AssessmentScheduleStatus.PUBLISHED);
        }
    }

    @Test
    void should_not_report_started_or_ended_when_dates_missing() {
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, null, null);

        assertThat(schedule.hasStartedAt(start)).isFalse();
        assertThat(schedule.hasEndedAt(start)).isFalse();
        assertThat(schedule.isOngoingAt(start)).isFalse();
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status, Instant from, Instant to) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
