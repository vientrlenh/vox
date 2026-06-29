package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolRubricDetailsQuery(
        UUID schoolId,
        UUID rubricId
) {}