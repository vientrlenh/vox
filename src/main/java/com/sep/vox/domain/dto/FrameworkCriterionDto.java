package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.valueobject.Code;

public record FrameworkCriterionDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String name,
    String description
) {

    public static FrameworkCriterionDto toDto(FrameworkCriterion frameworkCriterion) {
        return new FrameworkCriterionDto(
            frameworkCriterion.getId(),
            frameworkCriterion.getFrameworkVersionId(),
            Code.valueOf(frameworkCriterion.getCode()),
            frameworkCriterion.getName(),
            frameworkCriterion.getDescription()
        );
    }
}
