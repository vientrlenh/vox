package com.sep.vox.domain.dto.personalization;

import java.util.UUID;

public record TopicSuggestionDto(
    UUID id,
    String suggestedTopicName,
    String interestDimension,
    double confidence,
    String reasonText,
    String status
) {
}
