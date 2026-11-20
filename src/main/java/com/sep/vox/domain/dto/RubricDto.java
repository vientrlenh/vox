package com.sep.vox.domain.dto;

import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricOwnerType;

public record RubricDto(
        UUID id,
        String code,
        String name,
        String description,
        UUID languageId,
        UUID frameworkId,
        RubricOwnerType ownerType,
        UUID schoolId
) {
}