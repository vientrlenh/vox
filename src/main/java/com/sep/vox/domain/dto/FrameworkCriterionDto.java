package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;

public record FrameworkCriterionDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String name,
    String description,
    int order,
    List<FrameworkCriterionBandDto> bands
) {
    public static FrameworkCriterionDto of(FrameworkCriterion criterion, List<FrameworkCriterionBand> bands) {
        var bandDtos = bands.stream()
            .map(b -> new FrameworkCriterionBandDto(
                b.getId(),
                b.getFrameworkCriterionId(),
                b.getFrameworkResultBandId(),
                b.getDescriptor(),
                b.getPositiveSignals(),
                b.getNegativeSignals()
            ))
            .toList();
        return new FrameworkCriterionDto(
            criterion.getId(),
            criterion.getFrameworkVersionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            criterion.getOrder(),
            bandDtos
        );
    }
}
