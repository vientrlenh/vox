package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionTopicDto(
    UUID id,
    UUID bankId,
    String topicName,
    String description
) {
}
