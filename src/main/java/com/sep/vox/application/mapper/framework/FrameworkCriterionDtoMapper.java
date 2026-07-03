package com.sep.vox.application.mapper.framework;

import java.util.List;

import com.sep.vox.domain.dto.FrameworkCriterionBandDto;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;

public final class FrameworkCriterionDtoMapper {

    public static FrameworkCriterionDto toDto(
            FrameworkCriterion criterion,
            List<FrameworkCriterionBand> bands) {
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
