package com.sep.vox.application.query.dto;

import com.sep.vox.domain.model.school.SchoolGradeStatus;
import java.util.UUID;

/** A school year (school_grades) shown in the assessment directory. */
public record AssessmentDirectoryGradeInfo(
    UUID id,
    String code,
    String name,
    SchoolGradeStatus status
) {
    // Auxiliary constructor for the JPQL "SELECT NEW ...AssessmentDirectoryGradeInfo(...)" projection: the status is a plain String
    // column on the JPA entity, so this overload takes the raw string and converts it here, keeping the record itself enum-typed.
    public AssessmentDirectoryGradeInfo(UUID id, String code, String name, String status) {
        this(id, code, name, SchoolGradeStatus.valueOf(status));
    }
}
