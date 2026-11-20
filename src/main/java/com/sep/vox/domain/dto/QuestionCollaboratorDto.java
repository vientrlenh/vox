package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestionCollaboratorDto(
    UUID id,
    UUID userId,
    UUID questionId,
    String permission,
    Instant assignedAt
) {
}
