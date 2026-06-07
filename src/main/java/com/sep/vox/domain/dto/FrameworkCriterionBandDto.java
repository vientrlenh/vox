package com.sep.vox.domain.dto;

import java.util.UUID;

public record FrameworkCriterionBandDto(
    UUID id,
    UUID frameworkCriterionId,
    UUID frameworkResultBandId,
    String descriptor,
    String positiveSignals,
    String negativeSignals
) {
}
