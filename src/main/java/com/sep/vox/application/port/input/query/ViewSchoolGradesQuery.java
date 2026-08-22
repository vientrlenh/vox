package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolGradesQuery(
        UUID schoolId,
        UUID gradeLevelId,
        String status,
        int page,
        int size
) {
}