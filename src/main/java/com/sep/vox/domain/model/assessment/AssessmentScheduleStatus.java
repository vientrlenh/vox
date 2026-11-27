package com.sep.vox.domain.model.assessment;

public enum AssessmentScheduleStatus {
    DRAFT,
    PUBLISHED,
    COMPLETED,
    MOVED,
    CANCELLED,
    DELETED;

    /**
     * A schedule that is soft-deleted or has had everyone moved to another schedule is treated as nonexistent: every
     * read screen (student schedule, proctor schedule, attendance) must skip it. CANCELLED is still shown so users know
     * the schedule was cancelled.
     */
    public boolean isRemoved() {
        return this == MOVED || this == DELETED;
    }

    /**
     * A schedule that is in effect: it will take place, or it really did take place. A cancelled/moved/deleted one is
     * not -- every business rule that says "every schedule must have..." implicitly means a schedule in effect, because
     * forcing a schedule that will never take place to have enough rooms/proctors only blocks users and does not protect
     * the exam.
     */
    public boolean isInEffect() {
        return !isRemoved() && this != CANCELLED;
    }

    /**
     * Only a schedule in effect can take attendance -- a cancelled/moved/deleted one cannot.
     */
    public boolean allowsAttendance() {
        return isInEffect();
    }

    /**
     * Schedules a student may see. DRAFT is a schedule that is not yet published -- i.e. the timetable is not finished --
     * so it must not be exposed to candidates; MOVED/DELETED have been replaced/deleted. CANCELLED is still shown so
     * the student knows the schedule was cancelled.
     */
    public boolean isVisibleToStudent() {
        return this == PUBLISHED || this == COMPLETED || this == CANCELLED;
    }
}
