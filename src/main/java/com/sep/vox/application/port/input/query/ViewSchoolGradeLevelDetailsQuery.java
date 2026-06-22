package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolGradeLevelDetailsQuery(
        UUID schoolId,
        UUID gradeLevelId
) {}