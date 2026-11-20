package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import java.time.Instant;
import java.util.UUID;

public record AssessmentDto(
    UUID id,
    UUID blueprintId,
    UUID blueprintVersionId,
    String code,
    String name,
    String description,
    UUID schoolId,
    UUID languageId,
    AssessmentKind kind,
    AssessmentDeliveryMode deliveryMode,
    AssessmentStatus status,
    Integer maxAttempt,
    Integer assessmentTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    Instant openAt,
    Instant closeAt,
    UUID assessmentPolicyId,
    boolean requiresOtp,
    AssessmentRequiredStreamType requiredStreamType,
    AssessmentStreamTypePermission streamTypePermission,
    Boolean papersLocked,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,

    /** AI confidence threshold as a percentage (0-100); null = the school did not set one. */
    java.math.BigDecimal aiConfidenceThresholdPercent
) {
}
