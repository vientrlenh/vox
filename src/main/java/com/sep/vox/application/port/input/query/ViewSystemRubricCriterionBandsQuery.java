package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSystemRubricCriterionBandsQuery(
        UUID criterionId,
        int page,
        int size
) {}