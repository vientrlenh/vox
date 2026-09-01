package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * Một trang bài AI chấm lỗi, kèm số đếm của hai nhóm định mức.
 *
 * <p>{@code retryLeftCount}/{@code noRetryLeftCount} ĐÃ áp bộ lọc kỳ thi nhưng CHƯA áp bộ lọc định
 * mức — chúng đứng trên chính hai nút lọc nên phải dự đoán đúng kết quả của cú bấm.
 *
 * <p>Khi không lọc kỳ nào, hai số này khớp đúng {@code unscored.aiFailedRetryLeft} và
 * {@code unscored.aiFailedNoRetryLeft} trên trang tổng quan: cùng một CTE sinh ra cả hai.
 */
public record SchoolGradingFailurePageResponse(
    List<SchoolGradingFailureResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    int retryLeftCount,
    int noRetryLeftCount
) {
}
