package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolRubricCriterionBandsQuery(
        UUID schoolId,
        UUID criterionId,
        int page,
        int size
) {}