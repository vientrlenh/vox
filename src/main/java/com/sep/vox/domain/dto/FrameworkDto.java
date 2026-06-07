package com.sep.vox.domain.dto;

import java.util.UUID;

public record FrameworkDto(
    UUID id,
    String code,
    String name,
    String description,
    boolean isActive,
    UUID currentVersionId,
    String createdAt,
    String updatedAt
) {
}
