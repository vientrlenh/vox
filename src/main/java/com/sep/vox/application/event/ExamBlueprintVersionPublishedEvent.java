package com.sep.vox.application.event;

import java.util.UUID;

public record ExamBlueprintVersionPublishedEvent(
    UUID schoolId,
    String blueprintCode,
    String blueprintName
) {
}
