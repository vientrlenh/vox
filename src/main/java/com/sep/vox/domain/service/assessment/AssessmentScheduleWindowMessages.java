package com.sep.vox.domain.service.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Shared error messages for the schedule time-window constraints, so that every place applying the same invariant
 * (create/edit schedule, edit assessment, recalculate answering time) reports it the same way.
 */
public final class AssessmentScheduleWindowMessages {

    private AssessmentScheduleWindowMessages() {
    }

    public static String tooShortForAssessmentTime(Assessment assessment) {
        return "The schedule duration must be greater than or equal to the answering time of the assessment ("
            + formatDuration(assessment.getAssessmentTimeDurationSecond()) + ")";
    }

    /** Shows the duration as "xx minutes yy seconds" instead of raw seconds, for readability. */
    private static String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null) {
            return "0 seconds";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes == 0) {
            return seconds + " seconds";
        }
        return seconds == 0 ? minutes + " minutes" : minutes + " minutes " + seconds + " seconds";
    }

    public static String outsideAssessmentWindow(Assessment assessment) {
        StringBuilder message = new StringBuilder("The schedule must lie within the opening and closing time of the assessment");
        if (assessment.getOpenAt() != null) {
            message.append(", opens at ").append(assessment.getOpenAt());
        }
        if (assessment.getCloseAt() != null) {
            message.append(", closes at ").append(assessment.getCloseAt());
        }
        return message.toString();
    }

    /**
     * A scheduled schedule is no longer long enough after the assessment changed its time window or answering time.
     * For CLASS_TEST the schedule IS the openAt/closeAt window, so the message must point at the closing time --
     * telling the user to "edit the schedule" is meaningless, especially when the error fires right when the class test is created.
     */
    public static String schedulesNoLongerFit(int count, Assessment assessment) {
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            return "The total answering time (" + formatDuration(assessment.getAssessmentTimeDurationSecond())
                + ") exceeds the set opening and closing window. Please extend the closing time.";
        }
        return "The answering time (" + formatDuration(assessment.getAssessmentTimeDurationSecond()) + ") exceeds the duration of "
            + count + " scheduled schedules. Please extend those schedules first.";
    }

    public static String schedulesOutsideNewWindow(int count) {
        return "There are " + count + " schedules outside the new opening and closing time of the assessment. "
            + "Please move those schedules first.";
    }
}
