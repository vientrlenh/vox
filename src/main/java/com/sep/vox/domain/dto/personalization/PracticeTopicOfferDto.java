package com.sep.vox.domain.dto.personalization;

import java.util.UUID;

public record PracticeTopicOfferDto(
    UUID topicId,
    String name,
    String dimension,
    boolean savedByMe
) {
}
