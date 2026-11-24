package com.sep.vox.interfaces.graphql.input.practiceplanning;

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
