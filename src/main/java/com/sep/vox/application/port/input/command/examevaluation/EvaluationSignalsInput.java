package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;

public record EvaluationSignalsInput(
    Integer durationSeconds,
    Integer wordCount,
    Integer sentenceCount,
    Double lengthRatio,
    Integer expectedMinWords,
    Double asrConfidenceAvg,
    Double topicRelevanceScore,
    Double offTopicRatio,
    Double codeSwitchingRatio,
    Double speechRate,
    Double audioQuality,
    Double silenceRatio,
    String evidenceStatus,
    List<String> evidenceReasonCodes,
    ConfidenceCaseSignalsInput confidenceCase
) {
}
