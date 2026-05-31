package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionBankDto(
    UUID id,
    String bankName,
    String description,
    boolean isActive,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
