package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Toàn bộ bài của MỘT bài kiểm tra trên lớp, cho chính giáo viên tạo bài.
 *
 * <p>Phân trang 0-based, đồng bộ với các query cùng domain.
 *
 * @param unassignedOnly chỉ bài chưa có phân công đang mở — tức đúng những bài giáo viên
 *                       còn phải bấm "Nhận chấm"
 * @param search         khớp tên học sinh hoặc mã bài
 */
public record ViewClassTestGradingResultsQuery(
    UUID examId,
    String resultStatus,
    boolean unassignedOnly,
    String search,
    int page,
    int size
) {
}
