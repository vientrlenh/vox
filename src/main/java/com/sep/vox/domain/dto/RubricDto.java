package com.sep.vox.domain.dto;

import java.util.UUID;

public record RubricDto(
        UUID id,
        String code,
        String name,
        String description,
        UUID languageId,
        UUID frameworkId,
        String ownerType,
        UUID currentVersionId
) {
}