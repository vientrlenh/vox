package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentResultDecisionMethod;

public record UpdateAssessmentCommand(
    UUID assessmentId,
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer assessmentTimeDurationSecond,
    AssessmentResultDecisionMethod resultDecisionMethod,
    Boolean requiresOtp,
    /** null = giữ nguyên cấu hình giám sát; danh sách rỗng = tắt giám sát. */
    List<String> requiredStreamTypes,
    String streamTypePermission,

    /**
     * Ngưỡng tin cậy AI theo PHẦN TRĂM (0-100). null = giữ nguyên, đúng patch semantics như mọi
     * trường khác trong lệnh này.
     */
    java.math.BigDecimal aiConfidenceThresholdPercent
) {
}
