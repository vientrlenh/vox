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
) {
}
