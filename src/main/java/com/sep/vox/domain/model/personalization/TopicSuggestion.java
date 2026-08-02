package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TopicSuggestion(
    UUID id,
    UUID studentId,
    String suggestedTopicName,
    String keyword,
    String interestDimension,
    String curriculumGroup,
    BigDecimal confidence,
    String reasonText,
    String evidenceJson,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime respondedAt
) {
}
