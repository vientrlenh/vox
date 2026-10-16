package com.sep.vox.application.port.input.query;

public record SearchAssessmentAppealsQuery(
    String status,
    String keyword,
    int page,
    int size
) {
}
