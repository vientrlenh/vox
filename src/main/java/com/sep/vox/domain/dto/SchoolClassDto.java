package com.sep.vox.domain.dto;

import java.util.UUID;

public record SchoolClassDto(
    UUID id,
    UUID schoolId,
    UUID languageId,
    UUID schoolGradeId,
    String code,
    String name,
    String description,
    UUID targetSchoolLevelVersionId,
    String status,
    String createdAt, 
    String updatedAt
) {
}
