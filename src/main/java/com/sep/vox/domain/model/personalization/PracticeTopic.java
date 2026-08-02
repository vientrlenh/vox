package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PracticeTopic(
    UUID id,
    String name,
    String normalizedName,
    String description,
    String source,
    String interestDimension,
    String curriculumGroup,
    boolean active,
    OffsetDateTime createdAt,
    UUID sourceQuestionTopicId
) {
}
