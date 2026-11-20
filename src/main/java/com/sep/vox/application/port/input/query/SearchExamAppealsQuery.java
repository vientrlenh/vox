package com.sep.vox.application.port.input.query;

public record SearchExamAppealsQuery(
    String status,
    String keyword,
    int page,
    int size
) {
}
