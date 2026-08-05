package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Phân trang 0-based, đồng bộ với các query cùng domain exam-appeal.
 *
 * <p>Sau rework bảng này không còn khoá cứng ở PENDING_REVIEW nên có thêm các chiều
 * lọc của điều phối: trạng thái bài, vòng chấm, chưa gán, quá hạn, có đơn phúc khảo.
 *
 * @param kind loại bài; {@code null} được use case hiểu là {@code CENTRALIZED}. Mặc định
 *             nghiêng về kỳ thi tập trung vì đây là bảng điều phối của nhà trường — bài
 *             kiểm tra trên lớp chỉ vào đây khi client hỏi đích danh (màn theo dõi).
 */
public record SearchGradingAssignmentsQuery(
    UUID examId,
    UUID scheduleId,
    UUID teacherId,
    String resultStatus,
    String roundType,
    String status,
    boolean unassignedOnly,
    boolean overdueOnly,
    Boolean hasOpenAppeal,
    String search,
    String kind,
    int page,
    int size
) {
}
