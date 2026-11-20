package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một phiên chấm lỗi trong danh sách của một nhóm.
 *
 * <p>KHÔNG có điểm: phiên {@code GRADING_FAILED} không có dòng {@code exam_candidate_results} nào để
 * lấy điểm ra — xem javadoc của {@code HandOffGradingToHumanUseCase}. Thứ thay thế là những trường
 * giúp phân loại: đã thử mấy lần, chấm lại được không, đã có người nhận chưa.
 *
 * @param schoolName  {@code null} với kỳ thi cấp hệ thống ({@code exams.school_id} cho phép null)
 * @param retryable   kỳ thi chưa công bố điểm, tức {@code RetryGradingExamSessionUseCase} sẽ nhận;
 *                    kỳ đã ở {@code RESULTS_PUBLISHED} thì nó ném lỗi
 * @param handedOff   đã có dòng kết quả trỏ về phiên này, tức bài ĐÃ được đẩy sang hàng đợi người
 *                    chấm. Phiên vẫn ở {@code GRADING_FAILED} vì hand-off cố ý không đổi trạng thái,
 *                    nên nếu không đọc cờ này thì thao tác hàng loạt sẽ chấm đè lên bài giáo viên
 *                    đang chấm dở
 */
public record GradingFailureSessionDto(
    UUID sessionId,
    UUID schoolId,
    String schoolName,
    String schoolCode,
    UUID examId,
    String examName,
    String candidateName,
    Instant failedAt,
    Integer retryCount,
    String error,
    boolean retryable,
    boolean handedOff
) {
}
