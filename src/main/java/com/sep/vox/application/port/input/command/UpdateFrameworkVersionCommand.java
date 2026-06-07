package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
        List<CriterionBandInput> bands
    ) {}

    public record CriterionBandInput(
        String resultBandCode,
        String descriptor,
        String positiveSignals,
        String negativeSignals
    ) {}

    public record ResultBandInput(
        String code,
        String label,
        String description,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        int order
    ) {}
}
