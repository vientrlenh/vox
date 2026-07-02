package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolGradesQuery(
        UUID schoolId,
        UUID schoolGradeLevelId,
        int page,
        int size
) {
}