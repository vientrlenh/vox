package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TopicInterestEvent(
    UUID topicId,
    UUID sessionId,
    double signal,
    OffsetDateTime occurredAt
) {
}
