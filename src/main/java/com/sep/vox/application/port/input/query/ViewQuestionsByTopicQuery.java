package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewQuestionsByTopicQuery(
    UUID topicId,
    int page,
    int size
) {
}
