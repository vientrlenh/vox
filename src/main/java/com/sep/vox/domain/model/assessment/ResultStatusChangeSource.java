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
    /** Admin chốt sổ hàng loạt cả kỳ thi. */
    ADMIN_BULK_FINALIZE,
    /** Kỳ thi chuyển RESULTS_PUBLISHED kéo theo kết quả sang FINAL/PASSED/FAILED. */
    ASSESSMENT_PUBLISH,
    SYSTEM;

    /** Nguồn tương ứng với vòng chấm — dùng khi ghi audit từ các use case của giáo viên. */
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
