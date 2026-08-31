package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSessionStatus;

/**
 * @param gradingFailure chi tiết lần chấm hỏng; chỉ có nghĩa khi {@code status = GRADING_FAILED}, và
 *                       {@code null} ở đó nghĩa là "hỏng nhưng không biết vì sao" (nhánh DLT), chứ
 *                       không phải "chưa điền".
 */
public record UpdateExamSessionStatusCommand(
    UUID sessionId,
    ExamSessionStatus status,
    GradingFailure gradingFailure
) {

    /**
     * @param error      thông điệp thô của dịch vụ chấm, nguyên văn — nó là thứ duy nhất phân biệt
     *                   được một sự cố dịch vụ với hàng nghìn lỗi lẻ
     * @param retryCount số lần dịch vụ đã thử trước khi bỏ cuộc
     */
    public record GradingFailure(String error, Integer retryCount) {
    }

    /**
     * Dạng gọn cho mọi chuyển trạng thái KHÔNG phải chấm lỗi — phần lớn nơi gọi (vào lại phòng thi,
     * hết giờ, nộp bài) không có gì để nói về việc chấm, và bắt chúng truyền {@code null} chỉ làm
     * nhiễu chỗ gọi.
     */
    public UpdateExamSessionStatusCommand(UUID sessionId, ExamSessionStatus status) {
        this(sessionId, status, null);
    }

    public static UpdateExamSessionStatusCommand gradingFailed(UUID sessionId, String error, Integer retryCount) {
        return new UpdateExamSessionStatusCommand(
            sessionId,
            ExamSessionStatus.GRADING_FAILED,
            new GradingFailure(error, retryCount)
        );
    }

    /** Chấm hỏng mà không có lý do nào để lưu — bản tin đã hết đường retry và rơi xuống DLT. */
    public static UpdateExamSessionStatusCommand gradingFailedWithoutReason(UUID sessionId) {
        return new UpdateExamSessionStatusCommand(sessionId, ExamSessionStatus.GRADING_FAILED, null);
    }
}
