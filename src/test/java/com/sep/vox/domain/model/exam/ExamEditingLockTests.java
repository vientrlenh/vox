package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExamEditingLockTests {

    @Test
    void should_not_lock_exam_before_it_starts() {
        assertThat(examWithStatus(ExamStatus.DRAFT).isLockedForEditing()).isFalse();
        assertThat(examWithStatus(ExamStatus.SCHEDULED).isLockedForEditing()).isFalse();
    }

    @Test
    void should_lock_exam_from_in_progress_onwards() {
        assertThat(examWithStatus(ExamStatus.IN_PROGRESS).isLockedForEditing()).isTrue();
        assertThat(examWithStatus(ExamStatus.CLOSED).isLockedForEditing()).isTrue();
        assertThat(examWithStatus(ExamStatus.RESULTS_PUBLISHED).isLockedForEditing()).isTrue();
        assertThat(examWithStatus(ExamStatus.CANCELLED).isLockedForEditing()).isTrue();
    }

    @Test
    void should_not_lock_when_status_missing() {
        assertThat(examWithStatus(null).isLockedForEditing()).isFalse();
    }

    private Exam examWithStatus(ExamStatus status) {
        var exam = new Exam();
        exam.setStatus(status);
        return exam;
    }
}
