package com.sep.vox.domain.model.assessment;

/**
 * The grading round of an assignment row. The four rounds share one table
 * {@code assessment_grading_assignments} because each attempt has only one grader per round, and all
 * four rounds share exactly the same four actions ({@link GradingOutcome}).
 *
 * <p>The round decides which status the attempt <em>must</em> be in and which actions are valid --
 * see the matrix in {@code GradingRoundPolicy}.
 */
public enum GradingRoundType {
    /** First grading of a PENDING_REVIEW result. Ends with the result being published. */
    INITIAL,
    /** Spot check of a result already RELEASED. The result stays RELEASED throughout. */
    SPOT_CHECK,
    /** Re-examination of an INVALID result: keep it invalid, or lift the invalidation and then grade. */
    REMEDIATION,
    /** Appeal grading according to the student's appeal. Submitting is publishing. */
    APPEAL
}
