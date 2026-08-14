package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamDto(
    UUID id,
    UUID blueprintId,
    UUID blueprintVersionId,
    String code,
    String name,
    String description,
    UUID schoolId,
    UUID languageId,
    String kind,
    String deliveryMode,
    String status,
    Integer maxAttempt,
    Integer examTimeDurationSecond,
    String resultDecisionMethod,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    boolean requiresOtp,
    String requiredStreamType,
    String streamTypePermission,
    Boolean papersLocked,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy,

    /** Ngưỡng tin cậy AI theo phần trăm (0-100); null = nhà trường không đặt. */
    java.math.BigDecimal aiConfidenceThresholdPercent
) {
}
