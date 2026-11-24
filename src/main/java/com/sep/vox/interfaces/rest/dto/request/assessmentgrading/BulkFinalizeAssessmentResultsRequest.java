package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * @param releasePendingWithAiScores confirms publishing submissions nobody has graded, using the AI scores.
 *        Empty = {@code false}: the safe default, the use case refuses if unfinished submissions remain.
 */
public record BulkFinalizeAssessmentResultsRequest(
    @NotNull(message = "Thiếu kỳ thi cần chốt sổ")
    UUID assessmentId,

    Boolean releasePendingWithAiScores
) {
}
