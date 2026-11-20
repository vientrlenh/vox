package com.sep.vox.application.query.dto;

import java.util.UUID;

/** Niên khóa (school_grades) hiển thị trong danh bạ kỳ thi. */
public record AssessmentDirectoryGradeInfo(
    UUID id,
    String code,
    String name,
    String status
) {
}
