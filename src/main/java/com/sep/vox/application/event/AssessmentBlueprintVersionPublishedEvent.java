package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

/**
 * A blueprint version has just been published. Every SCHOOL_ADMIN of the school is a recipient.
 *
 * @param blueprintId together with {@code versionId}, builds the link to exactly the published
 *        version; {@code blueprintCode} is for display only, no screen looks anything up by code
 */
public record AssessmentBlueprintVersionPublishedEvent(
    List<UUID> schoolAdminIds,
    String blueprintCode,
    String blueprintName,
    UUID blueprintId,
    UUID versionId
) {
}
