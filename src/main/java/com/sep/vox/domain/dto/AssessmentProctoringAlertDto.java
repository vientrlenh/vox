package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;

/**
 * Một cảnh báo giám sát, cho màn chấm bài và cho lịch sử của màn giám sát trực tiếp.
 *
 * <p>{@code capturedAt} là thời gian TUYỆT ĐỐI, không phải mốc tua trong video. Quy nó về offset cần
 * thời điểm bắt đầu của bản ghi, thứ hiện chưa được lưu ở đâu -- xem ghi chú trong
 * {@code ViewAssessmentSessionProctoringAlertsUseCase}.
 *
 * <p>{@code candidateId} có thể null: nguồn phát không phải lúc nào cũng biết thí sinh nào, và ở đây
 * để trống được coi là trung thực hơn là đoán.
 */
public record AssessmentProctoringAlertDto(
    UUID id,
    String eventId,
    UUID assessmentSessionId,
    UUID candidateId,
    String streamId,
    String streamType,
    String alertType,
    String level,
    String source,
    String detail,
    BigDecimal confidence,
    Instant capturedAt,
    Instant raisedAt
) {

    public static AssessmentProctoringAlertDto toDto(AssessmentProctoringAlert assessmentProctoringAlert) {
        return new AssessmentProctoringAlertDto(
            assessmentProctoringAlert.getId(),
            assessmentProctoringAlert.getEventId(),
            assessmentProctoringAlert.getAssessmentSessionId(),
            assessmentProctoringAlert.getCandidateId(),
            assessmentProctoringAlert.getStreamId(),
            assessmentProctoringAlert.getStreamType(),
            assessmentProctoringAlert.getAlertType(),
            assessmentProctoringAlert.getLevel(),
            assessmentProctoringAlert.getSource(),
            assessmentProctoringAlert.getDetail(),
            assessmentProctoringAlert.getConfidence(),
            assessmentProctoringAlert.getCapturedAt(),
            assessmentProctoringAlert.getRaisedAt()
        );
    }
}
