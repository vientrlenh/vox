package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record FrameworkDto(
    UUID id,
    String code,
    String name,
    String description,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
