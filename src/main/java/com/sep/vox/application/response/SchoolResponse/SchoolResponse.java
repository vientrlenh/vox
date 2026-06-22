package com.sep.vox.application.response.SchoolResponse;


import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolResponse(
        UUID id,
        String schoolCode,
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String schoolDomain,
        String address,
        Integer studentCount,
        boolean isActive,
        OffsetDateTime createdAt,
        UUID createdBy,
        OffsetDateTime updatedAt,
        UUID updateBy
) {
}
