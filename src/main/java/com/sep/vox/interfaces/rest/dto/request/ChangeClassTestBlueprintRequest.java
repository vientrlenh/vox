package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

public record ChangeClassTestBlueprintRequest(
    UUID blueprintId,
    UUID blueprintVersionId
) {
}
