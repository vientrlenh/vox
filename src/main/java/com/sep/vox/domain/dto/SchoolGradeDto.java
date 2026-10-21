package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;

public record SchoolGradeDto(
    UUID id,
    UUID schoolId, 
    UUID gradeLevelId,
    String name,
    String description,
    UUID academicYearId, 
    SchoolGradeStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public static SchoolGradeDto toDto(SchoolGrade grade) {
        return new SchoolGradeDto(
            grade.getId(),
            grade.getSchoolId(), 
            grade.getGradeLevelId(), 
            grade.getName(),
            grade.getDescription(),
            grade.getAcademicYearId(), 
            grade.getStatus(),
            grade.getCreatedAt(),
            grade.getUpdatedAt()
        );
    }
}
