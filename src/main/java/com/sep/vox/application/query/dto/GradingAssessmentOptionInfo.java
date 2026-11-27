package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * One entry in the assessment filter of the teacher queue.
 *
 * <p>Deliberately thin: this is a dropdown, not an assessment screen. Every field added here is one
 * more field a teacher can read about a centralized assessment that they are not a member of.
 */
public record GradingAssessmentOptionInfo(
    UUID id,
    /** The assessment code -- names repeat between sessions, codes do not. */
    String code,
    String name,
    /** The caller's total assignments in this assessment, all rounds, all statuses. */
    long taskCount,
    /** Of which still ASSIGNED -- the number for the "still to grade" badge. */
    long openTaskCount
) {
}
