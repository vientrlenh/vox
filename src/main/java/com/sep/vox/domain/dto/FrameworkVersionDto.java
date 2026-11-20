package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public record FrameworkVersionDto(
    UUID id,
    UUID frameworkId,
    String code,
    String name,
    String description,
    int version,
    Instant effectiveFrom,
    Instant effectiveTo,
    FrameworkVersionStatus status,
    Instant createdAt,
    Instant updatedAt,
    List<FrameworkCriterionDto> criteria,
    List<FrameworkResultBandDto> resultBands
) {
}
