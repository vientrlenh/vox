package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

public record FrameworkCriterionDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String name,
    String description,
    int order,
    List<FrameworkCriterionBandDto> bands
) {
}
