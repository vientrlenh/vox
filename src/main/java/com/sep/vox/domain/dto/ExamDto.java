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
    String resultDecisionMethod,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Boolean papersLocked,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
