package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record RubricCriterionBandsKey(
        UUID criterionId,
        int page,
        int size
) {}