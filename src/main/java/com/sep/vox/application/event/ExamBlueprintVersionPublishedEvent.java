package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

public record ExamBlueprintVersionPublishedEvent(
    List<UUID> schoolAdminIds,
    String blueprintCode,
    String blueprintName
) {
}
