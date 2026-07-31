package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record SchoolRoomFromDto(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        boolean isActive,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updateBy
) {
}