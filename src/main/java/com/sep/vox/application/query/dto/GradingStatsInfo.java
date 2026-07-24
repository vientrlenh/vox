package com.sep.vox.application.query.dto;

/**
 * Thẻ số đầu màn phân công. {@code totalToGrade} CHỈ đếm bài đang PENDING_REVIEW —
 * bài đã RELEASED không chấm lại được nên không nằm trong khối lượng cần chấm.
 *
 * <p>Không có ô "đã chấm xong": bài chấm xong rời PENDING_REVIEW nên biến khỏi phạm
 * vi màn này (số sẽ luôn ~0). Việc chấm xong của giáo viên xem ở màn của họ.
 */
public record GradingStatsInfo(
    int totalToGrade,
    int unassigned,
    int assigned
) {
}
