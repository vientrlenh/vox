package com.sep.vox.domain.model.assessment;

/**
 * Lifecycle of a grading assignment. Deliberately only two statuses: the MVP submits the whole paper
 * in one go, so there is no "grading x/n" data for IN_PROGRESS to describe.
 * Removing an assignment deletes the row; it is not a status.
 */
public enum GradingAssignmentStatus {
    ASSIGNED,
    COMPLETED
}
