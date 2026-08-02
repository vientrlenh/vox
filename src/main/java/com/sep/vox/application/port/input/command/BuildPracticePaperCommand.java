package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record BuildPracticePaperCommand(
    UUID topicId,
    String origin,
    String fromSubAttribute,
    List<UUID> offeredTopicIds,
    List<UUID> previousOfferedTopicIds
) {
}
