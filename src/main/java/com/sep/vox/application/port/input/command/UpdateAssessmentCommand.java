package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;

public record UpdateAssessmentCommand(
    UUID assessmentId,
    String name,
    String description,
    Instant openAt,
    Instant closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer assessmentTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    Boolean requiresOtp,
    /** null = giữ nguyên cấu hình giám sát; danh sách rỗng = tắt giám sát. */
    List<String> requiredStreamTypes,
    AssessmentStreamTypePermission streamTypePermission,

    /**
     * Ngưỡng tin cậy AI theo PHẦN TRĂM (0-100). null = giữ nguyên, đúng patch semantics như mọi
     * trường khác trong lệnh này.
     */
    BigDecimal aiConfidenceThresholdPercent
) {
}
