package com.sep.vox.application.port.input.command.practiceevaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

public record RecordPracticeAttemptEvaluationCommand(
    UUID practiceResponseId,
    boolean validForScoring,
    ConfidenceCaseSignals confidenceCase,
    BigDecimal audioQuality,
    BigDecimal codeSwitchingRatio,
    int wordCount,
    List<PracticeCriterionScoreInput> criteria,
    String evaluatedAt
    // Before, there were also rawCriteria / turns / signals -- three raw slices that only served deriving weakness
    // observations. The weakness profile has been removed and the use case does not read them, so they were dropped from the command.
    // The Kafka wire DTO is unchanged: Python still publishes everything, Java ignores it.
) {
}
