package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;

/**
 * Confidence signals per case. Each value may be null when that branch
 * does not apply or cannot be measured for the current answer.
 */
public record ConfidenceCaseSignals(
    BigDecimal cAsrLog,
    BigDecimal qSnr,
    BigDecimal qSpeech,
    BigDecimal clippingRatio,
    BigDecimal cRef,
    BigDecimal cAlign,
    BigDecimal cAlignAccuracy,
    BigDecimal cAlignCoverage,
    BigDecimal cAlignTiming,
    BigDecimal cPfBranch,
    BigDecimal cGrammar,
    BigDecimal cVocabulary,
    BigDecimal cCoherence,
    BigDecimal grammarScoreDelta,
    BigDecimal vocabularyScoreDelta,
    BigDecimal coherenceScoreDelta
) {
}
