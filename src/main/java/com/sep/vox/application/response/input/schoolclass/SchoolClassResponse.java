package com.sep.vox.application.response.input.schoolclass;

import java.util.UUID;

public record SchoolClassResponse(
    UUID id,
    UUID schoolId,
    UUID languageId,
    UUID schoolGradeId,
    String code,
    String name,
    String description,
    String status,
    String createdAt,
    String updatedAt
) {
}
