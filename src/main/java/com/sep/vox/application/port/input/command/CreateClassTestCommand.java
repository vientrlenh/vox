package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateClassTestCommand(
    UUID schoolClassId,
    String name,
    String description,
    String openAt,
    String closeAt,
    List<UUID> questionIds,
    UUID existingBlueprintId,
    UUID existingBlueprintVersionId
) {
}
