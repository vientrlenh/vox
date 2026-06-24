package com.sep.vox.interfaces.graphql.mapper;

import java.util.List;

public record UpdateFrameworkVersionInput(
    String code,
    String name,
    String description,
    String effectiveFrom,
    String effectiveTo,
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
        List<SignalInput> positiveSignals,
        List<SignalInput> negativeSignals
    ) {}

    public record SignalInput(
        String code,
        String description,
        String importance,
        String evidenceHint
    ) {}

    public record ResultBandInput(
        String code,
        String label,
        String description,
        Double scoreMin,
        Double scoreMax,
        int order
    ) {}
}
