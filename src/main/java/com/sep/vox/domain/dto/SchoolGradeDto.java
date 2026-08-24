package com.sep.vox.domain.dto;

import java.time.LocalDate;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;

public record SchoolGradeDto(
    UUID id,
    UUID schoolId, 
    UUID gradeLevelId,
    String code,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    Instant createdAt,
    Instant updatedAt
) {

    public static SchoolGradeDto toDto(SchoolGrade grade) {
        return new SchoolGradeDto(
            grade.getId(),
            grade.getSchoolId(), 
            grade.getGradeLevelId(), 
            grade.getCode(),
            grade.getName(),
            grade.getDescription(),
            grade.getStartDate(),
            grade.getEndDate(),
            valueOf(grade.getStatus()),
            grade.getCreatedAt(),
            grade.getUpdatedAt()
        );
    }

    private static String valueOf(SchoolGradeStatus status) {
        return status == null ? null : status.name();
    }
}
