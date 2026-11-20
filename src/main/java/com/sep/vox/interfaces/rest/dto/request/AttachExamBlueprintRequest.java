package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.Valid;

public record AttachExamBlueprintRequest(
    UUID blueprintId,
    UUID blueprintVersionId,
    @Valid CreateBlueprintInlineRequest newBlueprint
) {
    public record CreateBlueprintInlineRequest(
        String code,
        String name,
        String description,
        UUID gradeLevelId,
        UUID languageId
    ) {
    }
}
