package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record UpdateFrameworkVersionCommand(
    UUID frameworkId,
    UUID versionId,
    String code,
    String name,
    String description,
    OffsetDateTime effectiveFrom,
    OffsetDateTime effectiveTo,
    List<CriterionInput> criteria,
    List<ResultBandInput> resultBands
) {
    public record CriterionInput(
        String code,
        String name,
        String description,
        int order,
        List<CriterionBandInput> bands
    ) {}

    public record CriterionBandInput(
        String resultBandCode,
        String descriptor,
        FrameworkCriterionSignals positiveSignals,
        FrameworkCriterionSignals negativeSignals
    ) {}

    public record ResultBandInput(
        String code,
        String label,
        String description,
        int order
    ) {}
}
