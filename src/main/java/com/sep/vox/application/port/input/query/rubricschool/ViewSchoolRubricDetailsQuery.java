package com.sep.vox.application.port.input.query.rubricschool;

import java.util.UUID;

public record ViewSchoolRubricDetailsQuery(
        UUID schoolId,
        UUID rubricId
) {}