package com.sep.vox.interfaces.kafka.dto;

/**
 * Agents báo chấm một câu luyện tập KHÔNG XONG.
 *
 * <p>Sự kiện này agents đã bắn từ lâu ({@code publish_practice_attempt_evaluation_failed}),
 * nhưng phía Java trước 2026-08-12 rơi vào nhánh "Skip unknown practice evaluation event type"
 * nên bị vứt thẳng. Hậu quả: chấm hỏng thì không ai biết, câu đó vĩnh viễn "chưa có bản chấm",
 * job quét bắn lại mỗi 5 phút, chấm hỏng lại -- vòng lặp vô hạn, và màn tổng kết quay mãi chờ
 * một kết quả sẽ không bao giờ tới. Bên THI thì xử lý đàng hoàng từ đầu
 * ({@code ExamAttemptEvaluationFailed}); chỉ bên luyện tập là thiếu.
 *
 * <p>Tên trường theo đúng thứ agents gửi: {@code practice_event_body} đổi
 * examAttemptId/answerId/questionId thành bộ practice* trước khi publish.
 */
public record PracticeAttemptEvaluationFailedEventDto(
    String eventType,
    Integer schemaVersion,
    String practiceSessionId,
    String practiceResponseId,
    String practiceQuestionId,
    PayloadDto payload
) {
    public record PayloadDto(
        String error,
        Integer retryCount
    ) {
    }
}
