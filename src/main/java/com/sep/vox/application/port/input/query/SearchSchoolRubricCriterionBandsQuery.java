package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record SearchSchoolRubricCriterionBandsQuery(
        UUID schoolId,
        UUID criterionId,
        String keyword,
        int page,
        int size
) {}