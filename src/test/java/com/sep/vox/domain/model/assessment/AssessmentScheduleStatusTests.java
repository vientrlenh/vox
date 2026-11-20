package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssessmentScheduleStatusTests {

    @Test
    void should_treat_draft_published_and_completed_as_in_effect() {
        assertThat(AssessmentScheduleStatus.DRAFT.isInEffect()).isTrue();
        assertThat(AssessmentScheduleStatus.PUBLISHED.isInEffect()).isTrue();
        assertThat(AssessmentScheduleStatus.COMPLETED.isInEffect()).isTrue();
    }

    @Test
    void should_not_treat_cancelled_moved_or_deleted_as_in_effect() {
        assertThat(AssessmentScheduleStatus.CANCELLED.isInEffect()).isFalse();
        assertThat(AssessmentScheduleStatus.MOVED.isInEffect()).isFalse();
        assertThat(AssessmentScheduleStatus.DELETED.isInEffect()).isFalse();
    }

    /**
     * Attendance is only a consequence of "the schedule is still valid" -- the two predicates must
     * never drift apart.
     */
    @Test
    void should_keep_attendance_aligned_with_in_effect() {
        for (AssessmentScheduleStatus status : AssessmentScheduleStatus.values()) {
            assertThat(status.allowsAttendance()).isEqualTo(status.isInEffect());
        }
    }
}
