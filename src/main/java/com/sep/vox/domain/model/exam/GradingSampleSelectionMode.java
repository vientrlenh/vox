package com.sep.vox.domain.model.exam;

/**
 * Cách admin chọn tập bài để giao tự động cho một {@link GradingRoundType vòng chấm}.
 *
 * <p>Bốn chế độ tồn tại vì bốn nhu cầu khác nhau: chấm lần đầu thì lấy hết, hậu kiểm
 * thì bốc mẫu (không ai hậu kiểm 100% bài), thanh tra thì nhắm bài đáng ngờ, và
 * xử lý ca lẻ thì chọn tay.
 */
public enum GradingSampleSelectionMode {
    /** Toàn bộ bài đủ điều kiện chưa có người. Mặc định cho vòng INITIAL. */
    ALL,
    /** Bốc ngẫu nhiên N% — dùng cho hậu kiểm định kỳ. */
    RANDOM_PERCENT,
    /** Ưu tiên bài rủi ro cao: AI kém tự tin, hoặc điểm sát ngưỡng đạt. */
    RISK_BASED,
    /** Danh sách bài do admin truyền vào. */
    MANUAL_LIST
}
