package com.sep.vox.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

public record SchoolGradeFromDto(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
}