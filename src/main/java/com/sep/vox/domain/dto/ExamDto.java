package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamDto(
    UUID id,
    UUID blueprintId,
    String code,
    String name,
    String description,
    UUID schoolId,
    UUID languageId,
    String kind,
    String status,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
