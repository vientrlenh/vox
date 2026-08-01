package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.UUID;

/**
 * Thẻ số đầu màn điều phối.
 *
 * <p>Khác bản cũ ở chỗ mẫu số là <em>toàn bộ bài của phạm vi đang xem</em>, không
 * còn bị khoá ở PENDING_REVIEW: sau rework admin điều phối cả bốn vòng nên phải thấy
 * được bức tranh đầy đủ, gồm cả bài đã công bố đang được hậu kiểm.
 *
 * <p>{@code byResultStatus} là <em>list</em> chứ không phải map: GraphQL không có
 * kiểu map, và một list các cặp có tên trường rõ ràng thì FE đọc thẳng được.
 *
 * @param unassigned      bài chưa có phân công đang mở
 * @param assigned        phân công đang mở
 * @param overdue         phân công đang mở đã quá hạn
 * @param teacherProgress tiến độ từng giáo viên, để admin biết ai đang nghẽn
 */
public record GradingStatsInfo(
    int total,
    List<ResultStatusCount> byResultStatus,
    int unassigned,
    int assigned,
    int overdue,
    List<TeacherProgress> teacherProgress
) {
    public record ResultStatusCount(
        String status,
        int count
    ) {
    }

    public record TeacherProgress(
        UUID teacherId,
        String teacherName,
        int assigned,
        int completed,
        int overdue
    ) {
    }
}
