package com.sep.vox.domain.dto.personalization;

import java.util.UUID;

public record TopicInterestDto(
    UUID topicId,
    String name,
    double score,
    int sessionsMentioned
) {
}
