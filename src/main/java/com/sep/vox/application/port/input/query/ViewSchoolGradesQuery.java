package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolGradesQuery(
        UUID schoolId,
        UUID gradeLevelId,
        String status,
        AppPageRequest pageRequest
) {
}