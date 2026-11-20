package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * Sức khỏe vận hành của nền tảng cho dashboard system admin.
 *
 * <p>Trộn hai loại thời gian một cách CÓ CHỦ Ý, và client phải hiển thị đúng như vậy:
 *
 * <ul>
 *   <li>{@code sessionsInProgress}, {@code examsInProgress}, {@code gradingQueueDepth} là ảnh chụp
 *       NGAY LÚC GỌI — chúng không đổi theo cửa sổ thời gian người dùng chọn. "Đang có 6 kỳ thi
 *       chạy" mà lại đổi khi bấm sang tháng trước thì vô nghĩa.
 *   <li>{@code graded}, {@code gradingFailed}, {@code successRatePercent}, {@code daily} thuộc
 *       CỬA SỔ đang xem.
 * </ul>
 */
public record PlatformOperationalHealthResponse(
    long sessionsInProgress,
    long examsInProgress,
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
