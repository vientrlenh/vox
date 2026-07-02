package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record SearchSystemRubricCriterionBandsQuery(
        UUID criterionId,
        String keyword,
        int page,
        int size
) {}