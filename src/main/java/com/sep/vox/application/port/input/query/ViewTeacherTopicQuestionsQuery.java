package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewTeacherTopicQuestionsQuery(
    UUID bankId,
    UUID topicId,
    int page,
    int size,
    String scope,
    String status,
    String type,
    String keyword
) {
}
