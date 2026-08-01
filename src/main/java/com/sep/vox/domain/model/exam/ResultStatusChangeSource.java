package com.sep.vox.domain.model.exam;

/**
 * Ai/cái gì đã đổi trạng thái một bài — cột {@code source} của
 * {@link ExamResultStatusHistory}.
 *
 * <p>Bốn giá trị {@code TEACHER_*} bám đúng {@link GradingRoundType} để tra ngược ra
 * vòng chấm mà không phải join sang bảng phân công.
 */
public enum ResultStatusChangeSource {
    /** AI chấm tự động (RecordExamAttemptEvaluation / Upsert...). */
    AI_EVALUATION,
    TEACHER_INITIAL,
    TEACHER_SPOT_CHECK,
    TEACHER_REMEDIATION,
    TEACHER_APPEAL,
    /** Admin chốt sổ hàng loạt cả kỳ thi. */
    ADMIN_BULK_FINALIZE,
    /** Kỳ thi chuyển RESULTS_PUBLISHED kéo theo kết quả sang FINAL/PASSED/FAILED. */
    EXAM_PUBLISH,
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
