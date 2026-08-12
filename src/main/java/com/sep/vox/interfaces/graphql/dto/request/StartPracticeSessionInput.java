package com.sep.vox.interfaces.graphql.dto.request;

import java.util.List;
import java.util.UUID;

public record StartPracticeSessionInput(
        UUID topicId,
        UUID targetFrameworkBandId,
        String origin,
        String fromSubAttribute,
        List<UUID> offeredTopicIds,
        List<UUID> previousOfferedTopicIds) {
}
