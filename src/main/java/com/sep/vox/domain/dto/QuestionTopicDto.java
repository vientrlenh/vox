package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionTopicDto(
    UUID id,
    UUID questionBankId,
    String code,
    String name,
    String description,
    String status, 
    String createdAt,
    String updatedAt
) {
}
