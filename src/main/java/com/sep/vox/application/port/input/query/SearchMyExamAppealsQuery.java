package com.sep.vox.application.port.input.query;

public record SearchMyExamAppealsQuery(
    String status,
    int page,
    int size
) {
}
