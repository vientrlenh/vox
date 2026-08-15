package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một cảnh báo giám sát, cho màn chấm bài và cho lịch sử của màn giám sát trực tiếp.
 *
 * <p>{@code capturedAt} là thời gian TUYỆT ĐỐI, không phải mốc tua trong video. Quy nó về offset cần
 * thời điểm bắt đầu của bản ghi, thứ hiện chưa được lưu ở đâu -- xem ghi chú trong
 * {@code ViewExamSessionProctoringAlertsUseCase}.
 *
 * <p>{@code candidateId} có thể null: nguồn phát không phải lúc nào cũng biết thí sinh nào, và ở đây
 * để trống được coi là trung thực hơn là đoán.
 */
public record ExamProctoringAlertDto(
    UUID id,
    String eventId,
    UUID examSessionId,
    UUID candidateId,
    String streamId,
    String streamType,
    String alertType,
    String level,
    String source,
    String detail,
    BigDecimal confidence,
    String capturedAt,
    String raisedAt
) {
}
