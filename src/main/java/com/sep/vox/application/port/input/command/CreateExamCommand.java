package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

public record CreateExamCommand(
    String code,
    String name,
    String description,
    UUID languageId,
    UUID blueprintId,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer examTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    Boolean requiresOtp,
    List<String> requiredStreamTypes,
    String streamTypePermission,
    String deliveryMode,

    /**
     * Ngưỡng tin cậy AI (0.00-1.00). NULL = không đặt, hệ thống dùng luật cứng như cũ.
     *
     * <p>Đặt ngưỡng thì bản chấm có overall_confidence thấp hơn sẽ sang PENDING_REVIEW, và toàn
     * bộ luật ngưỡng nội bộ bị bỏ qua -- xem RecordExamAttemptEvaluationUseCase.
     */
    java.math.BigDecimal aiConfidenceThresholdPercent
) {
}
