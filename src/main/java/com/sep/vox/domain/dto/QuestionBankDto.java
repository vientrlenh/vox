package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestionBankDto(
    UUID id,
    UUID languageId,
    UUID schoolId,
    String code,
    String name,
    String description,
    String ownerType,
    String status, 
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
