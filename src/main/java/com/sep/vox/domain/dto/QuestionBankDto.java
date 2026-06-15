package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionBankDto(
    UUID id,
    UUID languageId,
    String code,
    String bankName,
    String description,
    boolean isActive,
    String createdAt,
    String updatedAt
) {
    public String name() {
        return bankName;
    }

    public String status() {
        return isActive ? "PUBLISHED" : "ARCHIVED";
    }
}
