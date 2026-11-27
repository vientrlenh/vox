package com.sep.vox.domain.service.assessment;

import com.sep.vox.domain.model.assessment.Assessment;

/**
 * Locks editing of an assessment from the moment it starts (see {@link Assessment#isLockedForEditing()}).
 * Gathered in one place so every use case that creates/edits/deletes assessment information and schedules reports the same message.
 *
 * <p>Operational actions during the assessment -- publishing/completing/cancelling a session, getting the OTP, attendance,
 * grading -- deliberately do not use this guard.
 */
public final class AssessmentEditingGuard {

    private AssessmentEditingGuard() {
    }

    /** Assessment information: name, description, time window, assessment policy... */
    public static void requireAssessmentEditable(Assessment assessment) {
        if (assessment.isLockedForEditing()) {
            throw new IllegalArgumentException("The assessment cannot be edited once it has started");
        }
    }

    /** Schedule: schedules, proctors, placing candidates into schedules, assigning papers. */
    public static void requireScheduleEditable(Assessment assessment) {
        if (assessment.isLockedForEditing()) {
            throw new IllegalArgumentException("The schedule cannot be changed once the assessment has started");
        }
    }
}
