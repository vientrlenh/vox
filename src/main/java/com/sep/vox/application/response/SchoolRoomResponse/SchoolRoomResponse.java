package com.sep.vox.application.response.SchoolRoomResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolRoomResponse(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        boolean isActive,
        OffsetDateTime createdAt,
        UUID createdBy,
        OffsetDateTime updatedAt,
        UUID updateBy
) {
}