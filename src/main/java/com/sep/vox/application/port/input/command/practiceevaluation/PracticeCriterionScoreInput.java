package com.sep.vox.application.port.input.command.practiceevaluation;

/** Python may still send matchedBandCode, but practice does not assign bands so it is not accepted. */
public record PracticeCriterionScoreInput(String criterionCode, Double score) {
}
