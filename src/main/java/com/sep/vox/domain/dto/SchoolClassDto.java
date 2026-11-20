package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolClassStatus;

public record SchoolClassDto(
        UUID id,
        UUID schoolId,
        UUID languageId,
        UUID schoolGradeId,
        String code,
        String name,
        String description,
        SchoolClassStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
