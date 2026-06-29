package com.sep.vox.domain.dto;

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
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
