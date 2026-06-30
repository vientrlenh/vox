package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AttachExamBlueprintRequest(
    @NotNull(message = "ID blueprint không được để trống")
    UUID blueprintId
) {
}
