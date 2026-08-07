package com.sep.vox.application.port.input.command.practiceevaluation;

/** Python vẫn có thể gửi matchedBandCode, nhưng luyện tập không xếp loại nên không nhận. */
public record PracticeCriterionScoreInput(String criterionCode, Double score) {
}
