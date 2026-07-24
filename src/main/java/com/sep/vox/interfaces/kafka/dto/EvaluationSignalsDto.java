package com.sep.vox.interfaces.kafka.dto;

public record EvaluationSignalsDto(
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
    ConfidenceCaseSignalsDto confidenceCase
) {
}
