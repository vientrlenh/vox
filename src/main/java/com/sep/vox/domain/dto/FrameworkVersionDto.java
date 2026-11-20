package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

public record FrameworkVersionDto(
    UUID id,
    UUID frameworkId,
    String code,
    String name,
    String description,
    int version,
    String effectiveFrom,
    String effectiveTo,
    String status,
    String createdAt,
    String updatedAt,
    List<FrameworkCriterionDto> criteria,
    List<FrameworkResultBandDto> resultBands
) {
}
