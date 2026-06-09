package com.sep.vox.application.port.input.query;

public record ViewAdminQuestionsQuery(
    int page,
    int size,
    Boolean includeArchived,
    String status,
    String keyword
) {
}
