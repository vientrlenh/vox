package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolTopicQuestionsQuery(
    UUID bankId,
    UUID topicId,
    int page,
    int size,
    String status,
    String keyword
) {
}
