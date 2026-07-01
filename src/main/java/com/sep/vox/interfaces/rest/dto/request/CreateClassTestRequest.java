package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClassTestRequest(
    @NotNull(message = "Lop hoc la bat buoc")
    UUID schoolClassId,

    @NotBlank(message = "Ten bai kiem tra la bat buoc")
    String name,

    String description,
    String openAt,
    String closeAt,
    List<UUID> questionIds,
    UUID existingBlueprintId,
    UUID existingBlueprintVersionId
) {
}
