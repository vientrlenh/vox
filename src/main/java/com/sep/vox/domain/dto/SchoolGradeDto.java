package com.sep.vox.domain.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolGradeDto(
    UUID id,
    UUID schoolId,
    String code,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
