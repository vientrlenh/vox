package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionCollaboratorDto(
    UUID id,
    UUID userId,
    UUID questionId,
    String permission,
    String assignedAt
) {
}
