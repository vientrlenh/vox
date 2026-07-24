package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;

import java.util.List;

public final class FrameworkCriterionDtoMapper {

    public static FrameworkCriterionDto toDto(FrameworkCriterion criterion, List<FrameworkCriterionBand> bands) {
        return new FrameworkCriterionDto(
            criterion.getId(),
            criterion.getFrameworkVersionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            criterion.getOrder(),
            FrameworkCriterionBandDtoMapper.toDtoList(bands)
        );
    }
}
