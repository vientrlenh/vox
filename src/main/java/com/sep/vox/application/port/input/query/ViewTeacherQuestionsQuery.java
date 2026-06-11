package com.sep.vox.application.port.input.query;

public record ViewTeacherQuestionsQuery(
    int page,
    int size,
    String scope,
    String status,
    String type,
    String keyword
) {
}
