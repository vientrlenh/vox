package com.sep.vox.domain.model.exam;

/**
 * Kết luận của giáo viên khi đóng một dòng phân công.
 *
 * <p>Bốn hành động dùng chung cho cả bốn {@link GradingRoundType} — phát hiện then chốt
 * của bản rework: "release bài chưa sửa điểm" và "hậu kiểm rồi giữ nguyên" là cùng một
 * việc (xác nhận điểm hiện có đúng), nên gộp thành {@link #UPHELD}.
 *
 * <p>Tỉ lệ {@link #UPHELD} / {@link #REGRADED} ở vòng SPOT_CHECK chính là thước đo
 * chất lượng AI, không cần thu thập gì thêm.
 */
public enum GradingOutcome {
    /** Giữ nguyên điểm đang có. */
    UPHELD,
    /** Chấm lại, điểm thay đổi. */
    REGRADED,
    /** Kết luận vi phạm -> bài INVALID. Bắt buộc có lý do. */
    INVALIDATED,
    /** Kết luận không vi phạm -> gỡ INVALID. Bắt buộc có lý do. */
    CLEARED_INVALID,
    /** Giáo viên trả lại phân công (quen biết thí sinh...). Bắt buộc có lý do. */
    DECLINED
}
