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
    /** Chấm lần đầu bài PENDING_REVIEW. Kết thúc là bài được công bố. */
    INITIAL,
    /** Hậu kiểm bài đã RELEASED. Bài giữ nguyên RELEASED suốt quá trình. */
    SPOT_CHECK,
    /** Soi lại bài INVALID: giữ vô hiệu, hoặc gỡ vô hiệu rồi chấm lại. */
    REMEDIATION,
    /** Chấm phúc khảo theo đơn của học sinh. Nộp là công bố. */
    APPEAL
}
