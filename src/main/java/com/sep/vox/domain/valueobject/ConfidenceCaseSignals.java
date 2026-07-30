package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;

/**
 * Tín hiệu confidence theo từng case. Mỗi giá trị có thể null khi nhánh đó
 * không áp dụng hoặc không đo được cho câu trả lời hiện tại.
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
