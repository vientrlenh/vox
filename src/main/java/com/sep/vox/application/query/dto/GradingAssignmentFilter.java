package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Bộ lọc bảng phân công của school admin.
 *
 * <p>Gom thành record thay vì 11 tham số rời: bảng này lọc theo rất nhiều chiều
 * (bản cũ chỉ có 6 vì nó bị khoá cứng ở PENDING_REVIEW), và một danh sách tham số
 * dài toàn {@code UUID}/{@code String} là chỗ dễ truyền nhầm thứ tự nhất.
 *
 * @param resultStatus   trạng thái bài; {@code null} = mọi trạng thái. Bỏ ràng buộc
 *                       cứng PENDING_REVIEW của bản cũ là mục tiêu chính của rework.
 * @param roundType      lọc theo vòng chấm của dòng phân công
 * @param assignmentStatus {@code ASSIGNED} / {@code COMPLETED}; {@code null} = cả hai
 *                       và cả bài chưa gán ai
 * @param unassignedOnly chỉ bài chưa có phân công đang mở
 * @param overdueOnly    chỉ phân công còn mở đã quá hạn
 * @param hasOpenAppeal  {@code TRUE} = chỉ bài có đơn phúc khảo chưa kết thúc,
 *                       {@code FALSE} = chỉ bài không có, {@code null} = không lọc
 * @param keyword        khớp tên giáo viên, tên học sinh hoặc mã bài
 * @param examKind       {@code CENTRALIZED} / {@code CLASS_TEST}; {@code null} = cả hai.
 *                       Hai loại bài đi hai màn khác nhau — nhà trường điều phối kỳ thi
 *                       tập trung, còn bài trên lớp do chính giáo viên tạo bài tự chấm —
 *                       nên bảng nào cũng phải chốt loại bài của mình thay vì trộn chung.
 */
public record GradingAssignmentFilter(
    UUID schoolId,
    UUID examId,
    UUID scheduleId,
    UUID teacherId,
    String resultStatus,
    String roundType,
    String assignmentStatus,
    boolean unassignedOnly,
    boolean overdueOnly,
    Boolean hasOpenAppeal,
    String keyword,
    String examKind
) {
}
