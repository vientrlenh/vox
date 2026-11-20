package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một bài AI chấm lỗi mà CHƯA ai xử lý, nhìn từ phía nhà trường.
 *
 * <p>Không có điểm: phiên {@code GRADING_FAILED} chưa từng sinh dòng {@code exam_candidate_results}
 * nào. Thứ thay thế là những trường giúp quyết định giữa hai lối ra — nhờ AI chấm lại hay xếp giáo
 * viên chấm tay.
 *
 * <p>Hai bộ đếm KHÁC NHAU, đừng gộp:
 *
 * <ul>
 *   <li>{@code aiRetryCount} là số lần dịch vụ chấm tự thử lại bên trong MỘT lượt, lấy từ payload
 *       Kafka. Nó nói về độ dai của sự cố, không nói ai đã bấm gì.
 *   <li>{@code schoolRetryLeft} suy từ {@code exam_sessions.school_regrade_count} — định mức một
 *       lượt nhờ AI chấm lại của nhà trường. Đây mới là thứ quyết định nút nào còn bấm được.
 * </ul>
 */
public record SchoolGradingFailureDto(
    UUID sessionId,
    UUID examId,
    String examCode,
    String examName,
    String candidateName,
    /** null khi học sinh chưa được xếp vào lớp đang hoạt động nào. */
    String className,
    /** Mốc nộp bài — phiên hỏng không có mốc nào khác đáng tin để xếp thứ tự. */
    Instant failedAt,
    /** Thông điệp lỗi thô; null với phiên bị đánh dấu hỏng qua nhánh DLT. */
    String error,
    /** Dịch vụ chấm đã tự thử mấy lần trước khi bỏ cuộc; null khi không rõ. */
    Integer aiRetryCount,
    /** Trường còn lượt nhờ AI chấm lại cho phiên này hay không. */
    boolean schoolRetryLeft
) {
}
