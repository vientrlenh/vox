package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.SchoolGradeFromDto;
import com.sep.vox.domain.model.school.SchoolGrade;

public class SchoolGradeDtoMapper {
    public static SchoolGradeFromDto toDto(SchoolGrade grade) {
        if (grade == null) return null;

        return new SchoolGradeFromDto(
                grade.getId(),
                grade.getSchoolId(),
                grade.getCode(),
                grade.getName(),
                grade.getDescription(),
                grade.getStartDate(),
                grade.getEndDate(),
                grade.getStatus() != null ? grade.getStatus().name() : null
        );
    }
}