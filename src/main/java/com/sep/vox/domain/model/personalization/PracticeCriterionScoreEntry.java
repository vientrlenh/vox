package com.sep.vox.domain.model.personalization;

public record PracticeCriterionScoreEntry(
    String criterionCode,
    double score,
    String matchedBandCode
) {
}
