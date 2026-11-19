package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExamScheduleStatusTests {

    @Test
    void should_treat_draft_published_and_completed_as_in_effect() {
        assertThat(ExamScheduleStatus.DRAFT.isInEffect()).isTrue();
        assertThat(ExamScheduleStatus.PUBLISHED.isInEffect()).isTrue();
        assertThat(ExamScheduleStatus.COMPLETED.isInEffect()).isTrue();
    }

    @Test
    void should_not_treat_cancelled_moved_or_deleted_as_in_effect() {
        assertThat(ExamScheduleStatus.CANCELLED.isInEffect()).isFalse();
        assertThat(ExamScheduleStatus.MOVED.isInEffect()).isFalse();
        assertThat(ExamScheduleStatus.DELETED.isInEffect()).isFalse();
    }

    /**
     * Attendance is only a consequence of "the schedule is still valid" -- the two predicates must
     * never drift apart.
     */
    @Test
    void should_keep_attendance_aligned_with_in_effect() {
        for (ExamScheduleStatus status : ExamScheduleStatus.values()) {
            assertThat(status.allowsAttendance()).isEqualTo(status.isInEffect());
        }
    }
}
