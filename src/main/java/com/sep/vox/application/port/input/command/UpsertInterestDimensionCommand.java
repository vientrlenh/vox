package com.sep.vox.application.port.input.command;

public record UpsertInterestDimensionCommand(
    String code,
    String label,
    String description,
    Boolean active,
    Boolean quizEligible,
    Integer displayOrder) {
}
