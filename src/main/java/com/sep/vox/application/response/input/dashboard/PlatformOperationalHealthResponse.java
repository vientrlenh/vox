package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * The operational health of the platform for the system admin dashboard.
 *
 * <p>Deliberately mixes two kinds of time, and the client must display it exactly that way:
 *
 * <ul>
 *   <li>{@code sessionsInProgress}, {@code assessmentsInProgress}, {@code gradingQueueDepth} are a snapshot
 *       RIGHT AT THE TIME OF THE CALL -- they do not change with the time window the user selects. "6 assessments
 *       are running now" changing when you click to last month is meaningless.
 *   <li>{@code graded}, {@code gradingFailed}, {@code successRatePercent}, {@code daily} belong to the
 *       window being viewed.
 * </ul>
 */
public record PlatformOperationalHealthResponse(
    long sessionsInProgress,
    long assessmentsInProgress,
    long gradingQueueDepth,
    long graded,
    long gradingFailed,
    /** {@code null} khi trong cửa sổ chưa có phiên nào chấm xong lẫn chấm lỗi — 0% và "chưa có dữ
     * liệu" là hai chuyện khác nhau, và một hệ thống chưa chạy phiên nào không phải là hệ thống có
     * tỷ lệ thành công 0%. */
    Double successRatePercent,
    /** Liên tục theo ngày, cũ -> mới, ngày không có phiên nào đã trả về 0. */
    List<GradingOutcomeBucketResponse> daily
) {
}
