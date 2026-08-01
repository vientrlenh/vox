package com.sep.vox.domain.model.exam;

/**
 * Vòng chấm của một dòng phân công. Bốn vòng dùng chung một bảng
 * {@code exam_grading_assignments} vì mỗi bài chỉ có một người chấm mỗi vòng, và cả
 * bốn vòng dùng chung đúng bốn hành động ({@link GradingOutcome}).
 *
 * <p>Vòng quyết định bài <em>đang</em> phải ở trạng thái nào và hành động nào hợp lệ —
 * xem ma trận ở {@code GradingRoundPolicy}.
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
