package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record AssignExamAppealReviewersCommand(
    UUID appealId,
    List<UUID> reviewerIds
) {
}
