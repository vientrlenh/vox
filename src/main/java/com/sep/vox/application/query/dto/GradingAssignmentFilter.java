package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * The filter of the school admin's assignment table.
 *
 * <p>Gathered into a record instead of 11 loose parameters: this table filters on many dimensions
 * (the old version had only 6 because it was hard-locked to PENDING_REVIEW), and a long parameter list
 * of {@code UUID}/{@code String} is the easiest place to pass things in the wrong order.
 *
 * @param resultStatus   status of the attempt; {@code null} = every status. Dropping the hard
 *                       PENDING_REVIEW constraint of the old version was the main goal of the rework.
 * @param roundType      filter by the grading round of the assignment row
 * @param assignmentStatus {@code ASSIGNED} / {@code COMPLETED}; {@code null} = both
 *                       and also attempts nobody has been assigned
 * @param unassignedOnly only attempts with no open assignment
 * @param overdueOnly    only open assignments that are past due
 * @param hasOpenAppeal  {@code TRUE} = only attempts with an unfinished appeal,
 *                       {@code FALSE} = only attempts without one, {@code null} = no filter
 * @param keyword        matches teacher name, student name or attempt code
 * @param assessmentKind {@code CENTRALIZED} / {@code CLASS_TEST}; {@code null} = both.
 *                       The two kinds go to two different screens -- the school coordinates the centralized
 *                       assessment, while a class test is graded by its own creating teacher --
 *                       so every table must fix its own kind instead of mixing them.
 */
public record GradingAssignmentFilter(
    UUID schoolId,
    UUID assessmentId,
    UUID scheduleId,
    UUID teacherId,
    String resultStatus,
    String roundType,
    String assignmentStatus,
    boolean unassignedOnly,
    boolean overdueOnly,
    Boolean hasOpenAppeal,
    String keyword,
    String assessmentKind
) {
}
