package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

/**
 * Toàn bộ bài của MỘT bài kiểm tra trên lớp, cho chính giáo viên tạo bài.
 *
 * <p>Trang đếm từ 1 ({@link AppPageRequest}); use case đổi sang 0-based bằng {@code repositoryPage()} khi gọi repository.
 *
 * @param unassignedOnly chỉ bài chưa có phân công đang mở — tức đúng những bài giáo viên
 *                       còn phải bấm "Nhận chấm"
 * @param search         khớp tên học sinh hoặc mã bài
 */
public record ViewClassTestGradingResultsQuery(
    UUID assessmentId,
    String resultStatus,
    boolean unassignedOnly,
    String search,
    AppPageRequest pageRequest
) {
}
