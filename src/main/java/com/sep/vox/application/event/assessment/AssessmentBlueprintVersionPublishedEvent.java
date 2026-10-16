package com.sep.vox.application.event.assessment;

import java.util.List;
import java.util.UUID;

public record AssessmentBlueprintVersionPublishedEvent(
    List<UUID> schoolAdminIds,
    String blueprintCode,
    String blueprintName,
    UUID blueprintId,
    UUID versionId
) {
}
