package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolRubricResultBandsQuery(
        UUID schoolId,
        UUID versionId,
        int page,
        int size
) {}