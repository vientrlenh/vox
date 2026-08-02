package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TopicInterestScoreEntry(
    UUID topicId,
    double score,
    int sessionCount,
    OffsetDateTime lastEventAt
) {
}
