package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionBankDto(
    UUID id,
    UUID languageId,
    String code,
    String name,
    String description,
    String status, 
    String createdAt,
    String updatedAt
) {
}
