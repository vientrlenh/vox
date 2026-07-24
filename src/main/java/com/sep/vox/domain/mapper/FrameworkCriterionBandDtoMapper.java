package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.FrameworkCriterionBandDto;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;

import java.util.List;

public final class FrameworkCriterionBandDtoMapper {

    public static FrameworkCriterionBandDto toDto(FrameworkCriterionBand band) {
        return new FrameworkCriterionBandDto(
            band.getId(),
            band.getFrameworkCriterionId(),
            band.getFrameworkResultBandId(),
            band.getDescriptor(),
            band.getPositiveSignals(),
            band.getNegativeSignals()
        );
    }

    public static List<FrameworkCriterionBandDto> toDtoList(List<FrameworkCriterionBand> bands) {
        return bands.stream()
            .map(FrameworkCriterionBandDtoMapper::toDto)
            .toList();
    }
}
