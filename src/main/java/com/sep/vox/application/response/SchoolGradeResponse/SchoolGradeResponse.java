package com.sep.vox.application.response.SchoolGradeResponse;


import com.sep.vox.domain.model.school.SchoolGradeStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolGradeResponse(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        SchoolGradeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID createdBy,
        UUID updatedBy
        ) {
}
