package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

public record AttachExamBlueprintRequest(
    UUID blueprintId,
    UUID blueprintVersionId
) {
}
