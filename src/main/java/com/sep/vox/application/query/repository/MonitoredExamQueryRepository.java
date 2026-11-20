package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.MonitoredExamSummary;

/**
 * Đường đọc riêng của màn giám sát: kỳ thi mà một người CÓ QUYỀN GIÁM SÁT.
 *
 * <p>Quyền ở đây suy ra từ chính ca thi chứ không từ hội đồng, giống hệt
 * {@code ProctorScheduleAccessService}: được phân công gác một ca là được thấy kỳ thi chứa nó. Nhờ
 * vậy màn giám sát không phải nới quyền của {@code ExamRepository.findAccessible} -- vốn là đường
 * vào của màn QUẢN LÝ kỳ thi, nới ở đó là mở luôn dashboard cho giám thị.
 */
public interface MonitoredExamQueryRepository {

    /**
     * @param examId    lọc về đúng một kỳ thi; null = mọi kỳ thi giám sát được.
     * @param now       mốc hiện tại, luôn cần vì {@code liveScheduleCount} đếm theo nó.
     * @param leadUntil chỉ lấy ca bắt đầu trước mốc này và chưa kết thúc; null = BỎ lọc thời gian.
     *                  Null dùng cho đường đọc một kỳ thi cụ thể: mở lại trang một ca đã xong vẫn
     *                  phải thấy tên kỳ thi, nếu không thì đầu trang trống trong khi danh sách ca
     *                  bên dưới vẫn liệt kê bình thường.
     */
    List<MonitoredExamSummary> findMonitorableByTeacher(UUID teacherId, UUID examId, Instant now, Instant leadUntil);

    /** Bản cho school admin: mọi kỳ thi của trường, không cần được phân công ca nào. */
    List<MonitoredExamSummary> findMonitorableBySchool(UUID schoolId, UUID examId, Instant now, Instant leadUntil);
}
