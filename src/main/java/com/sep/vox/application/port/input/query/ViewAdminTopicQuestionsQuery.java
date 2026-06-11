package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAdminTopicQuestionsQuery(
    UUID bankId,
    UUID topicId,
    int page,
    int size,
    Boolean includeArchived,
    String scope,
    String status,
    String type,
    String keyword
) {
}
