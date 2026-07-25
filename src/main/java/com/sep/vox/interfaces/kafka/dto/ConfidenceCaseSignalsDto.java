package com.sep.vox.interfaces.kafka.dto;

public record ConfidenceCaseSignalsDto(
    Double cAsrLog,
    Double qSnr,
    Double qSpeech,
    Double clippingRatio,
    Double cRef,
    Double cAlign,
    Double cAlignAccuracy,
    Double cAlignCoverage,
    Double cAlignTiming,
    Double cPfBranch,
    Double cGrammar,
    Double cVocabulary,
    Double cDiscourse,
    Double grammarScoreDelta,
    Double vocabularyScoreDelta,
    Double discourseScoreDelta
) {
}
