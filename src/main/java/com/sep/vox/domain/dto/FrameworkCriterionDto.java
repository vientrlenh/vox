package com.sep.vox.domain.dto;

import java.util.UUID;

public record FrameworkCriterionDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String name,
    String description,
    int order, 
    String createdAt, 
    String updatedAt
) {
}
