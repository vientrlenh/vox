package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PracticePaper(
    UUID id,
    UUID studentId,
    UUID practiceTopicId,
    String origin,
    String goalAtBuild,
    String offeredTopicIdsJson,
    String previousOfferedTopicIdsJson,
    int plannedSeconds,
    int reservedQuotaSeconds,
    OffsetDateTime expiresAt,
    String status,
    OffsetDateTime createdAt
) {
    public PracticePaper withStatus(String newStatus) {
        return new PracticePaper(
            id,
            studentId,
            practiceTopicId,
            origin,
            goalAtBuild,
            offeredTopicIdsJson,
            previousOfferedTopicIdsJson,
            plannedSeconds,
            reservedQuotaSeconds,
            expiresAt,
            newStatus,
            createdAt
        );
    }
}
