package com.sep.vox.application.response.input.schooluser;

public record SchoolUserImportError(
    int rowNumber,
    String field,
    String code,
    String message,
    String rawValue
) {
}
