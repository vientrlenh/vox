package com.sep.vox.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SupportedLanguageDto(
    UUID id,
    String code,
    String name,
    String description,
    boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
