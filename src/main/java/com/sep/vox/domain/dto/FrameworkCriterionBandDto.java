package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record FrameworkCriterionBandDto(
    UUID id,
    UUID frameworkCriterionId,
    UUID frameworkResultBandId,
    String descriptor,
    FrameworkCriterionSignals positiveSignals,
    FrameworkCriterionSignals negativeSignals
) {

    public static FrameworkCriterionBandDto toDto(FrameworkCriterionBand frameworkCriterionBand) {
        return new FrameworkCriterionBandDto(
            frameworkCriterionBand.getId(),
            frameworkCriterionBand.getFrameworkCriterionId(),
            frameworkCriterionBand.getFrameworkResultBandId(),
            frameworkCriterionBand.getDescriptor(),
            frameworkCriterionBand.getPositiveSignals(),
            frameworkCriterionBand.getNegativeSignals()
        );
    }

    public static List<FrameworkCriterionBandDto> toDtoList(List<FrameworkCriterionBand> bands) {
        return bands.stream()
            .map(FrameworkCriterionBandDto::toDto)
            .toList();
    }
}
