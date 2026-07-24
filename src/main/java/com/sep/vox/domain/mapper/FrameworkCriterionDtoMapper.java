package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.model.framework.FrameworkCriterion;

public final class FrameworkCriterionDtoMapper {

    public static FrameworkCriterionDto toDto(FrameworkCriterion criterion) {
        return new FrameworkCriterionDto(
            criterion.getId(),
            criterion.getFrameworkVersionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            criterion.getOrder(), 
            criterion.getCreatedAt().toString(), 
            criterion.getUpdatedAt().toString()
        );
    }
}
