package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record FrameworkCriterionBandDto(
    UUID id,
    UUID frameworkCriterionId,
    UUID frameworkResultBandId,
    String descriptor,
    FrameworkCriterionSignals positiveSignals,
    FrameworkCriterionSignals negativeSignals
) {
}
