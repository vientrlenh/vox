package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolGradesQuery(
        UUID schoolId,
        int page, 
        int size
) {
}