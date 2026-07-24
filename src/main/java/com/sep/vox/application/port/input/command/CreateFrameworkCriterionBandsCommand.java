package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record CreateFrameworkCriterionBandsCommand(
        UUID frameworkId,
        UUID versionId,
        UUID criterionId,
        List<CriterionBandItemCommand> bands
) {
    public record CriterionBandItemCommand(
            String resultBandCode,
            String descriptor,
            FrameworkCriterionSignals positiveSignals,
            FrameworkCriterionSignals negativeSignals
    ) {}
}
