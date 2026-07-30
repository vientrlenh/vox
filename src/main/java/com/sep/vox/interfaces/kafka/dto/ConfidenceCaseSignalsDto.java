package com.sep.vox.interfaces.kafka.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

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
    @JsonAlias("cDiscourse")
    Double cCoherence,
    Double grammarScoreDelta,
    Double vocabularyScoreDelta,
    @JsonAlias("discourseScoreDelta")
    Double coherenceScoreDelta
) {
}
