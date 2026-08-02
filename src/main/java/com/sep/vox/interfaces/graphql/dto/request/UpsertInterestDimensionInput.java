package com.sep.vox.interfaces.graphql.dto.request;

public record UpsertInterestDimensionInput(
    String code,
    String label,
    String description,
    Boolean active,
    Boolean quizEligible,
    Integer displayOrder) {
}
