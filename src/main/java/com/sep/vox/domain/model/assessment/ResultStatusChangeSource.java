package com.sep.vox.domain.model.assessment;

/**
 * Who/what changed the status of an attempt -- the {@code source} column of
 * {@link AssessmentResultStatusHistory}.
 *
 * <p>The four {@code TEACHER_*} values follow {@link GradingRoundType} exactly so the grading round can be looked up in reverse
 * without joining to the assignment table.
 */
public enum ResultStatusChangeSource {
    /** Automatic AI grading (RecordAssessmentAttemptEvaluation / Upsert...). */
    AI_EVALUATION,
    TEACHER_INITIAL,
    TEACHER_SPOT_CHECK,
    TEACHER_REMEDIATION,
    TEACHER_APPEAL,
    /** The admin finalizes the whole assessment in bulk. */
    ADMIN_BULK_FINALIZE,
    /** The assessment moves to RESULTS_PUBLISHED, taking results to FINAL/PASSED/FAILED with it. */
    ASSESSMENT_PUBLISH,
    SYSTEM;

    /** The source matching a grading round — used when writing the audit from teacher use cases. */
    public static ResultStatusChangeSource ofRound(GradingRoundType roundType) {
        if (roundType == null) {
            return SYSTEM;
        }
        return switch (roundType) {
            case INITIAL -> TEACHER_INITIAL;
            case SPOT_CHECK -> TEACHER_SPOT_CHECK;
            case REMEDIATION -> TEACHER_REMEDIATION;
            case APPEAL -> TEACHER_APPEAL;
        };
    }
}
