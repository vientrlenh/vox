package com.sep.vox.domain.dto;

import java.util.UUID;

public record SchoolGradeLevelDto(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        int order,
        String status,
        String createdAt,
        String updatedAt
) {
}
