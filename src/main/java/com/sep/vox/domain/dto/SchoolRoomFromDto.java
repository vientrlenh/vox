package com.sep.vox.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolRoomFromDto(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        Integer capacity,
        boolean isActive,
        OffsetDateTime createdAt,
        UUID createdBy,
        OffsetDateTime updatedAt,
        UUID updateBy
) {
}