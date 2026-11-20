package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestionTopicDto(
    UUID id,
    UUID questionBankId,
    String code,
    String name,
    String description,
    String status, 
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
