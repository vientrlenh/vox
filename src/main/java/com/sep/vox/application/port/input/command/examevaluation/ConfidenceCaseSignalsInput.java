package com.sep.vox.application.port.input.command.examevaluation;

public record ConfidenceCaseSignalsInput(
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
