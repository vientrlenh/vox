package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;

public final class SchoolGradeDtoMapper {

    public static SchoolGradeDto toSchoolGradeDto(SchoolGrade schoolGrade) {
        return new SchoolGradeDto(
            schoolGrade.getId(),
            schoolGrade.getSchoolGradeLevelId(),
            schoolGrade.getCode(),
            schoolGrade.getName(),
            schoolGrade.getDescription(),
            schoolGrade.getStartDate(),
            schoolGrade.getEndDate(),
            valueOf(schoolGrade.getStatus()),
            schoolGrade.getCreatedAt(),
            schoolGrade.getUpdatedAt()
        );
    }

    private static String valueOf(SchoolGradeStatus status) {
        return status == null ? null : status.name();
    }
}
