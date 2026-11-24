package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record UpdateFrameworkCriterionBandCommand(
        UUID frameworkId,
        UUID versionId,
        UUID criterionId,
        UUID bandId,
        String descriptor,
        boolean descriptorProvided,
        FrameworkCriterionSignals positiveSignals,
        FrameworkCriterionSignals negativeSignals
) {}
