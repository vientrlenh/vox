package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssessmentEditingLockTests {

    @Test
    void should_not_lock_assessment_before_it_starts() {
        assertThat(assessmentWithStatus(AssessmentStatus.DRAFT).isLockedForEditing()).isFalse();
        assertThat(assessmentWithStatus(AssessmentStatus.SCHEDULED).isLockedForEditing()).isFalse();
    }

    @Test
    void should_lock_assessment_from_in_progress_onwards() {
        assertThat(assessmentWithStatus(AssessmentStatus.IN_PROGRESS).isLockedForEditing()).isTrue();
        assertThat(assessmentWithStatus(AssessmentStatus.CLOSED).isLockedForEditing()).isTrue();
        assertThat(assessmentWithStatus(AssessmentStatus.RESULTS_PUBLISHED).isLockedForEditing()).isTrue();
        assertThat(assessmentWithStatus(AssessmentStatus.CANCELLED).isLockedForEditing()).isTrue();
    }

    @Test
    void should_not_lock_when_status_missing() {
        assertThat(assessmentWithStatus(null).isLockedForEditing()).isFalse();
    }

    private Assessment assessmentWithStatus(AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setStatus(status);
        return assessment;
    }
}
