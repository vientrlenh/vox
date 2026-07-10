package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;

public record EvaluationSignals(
    int durationSeconds,
    int wordCount,
    Integer sentenceCount,
    BigDecimal lengthRatio,
    Integer expectedMinWords,
    BigDecimal taskRelevance,
    BigDecimal offTopicRatio,
    BigDecimal codeSwitchingRatio,
    BigDecimal asrConfidence,
    BigDecimal aiConfidence,
    BigDecimal audioQuality,
    BigDecimal silenceRatio,
    BigDecimal speechRate
) {
    public EvaluationSignals {
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("Thời gian không được dưới 0");
        }
        if (wordCount < 0) {
            throw new IllegalArgumentException("Số từ không được dưới 0");
        }
        if (sentenceCount != null && sentenceCount < 0) {
            throw new IllegalArgumentException("Số câu không được dưới 0");
        }
        if (lengthRatio != null && lengthRatio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Length ratio không được dưới 0");
        }
        if (expectedMinWords != null && expectedMinWords < 0) {
            throw new IllegalArgumentException("Số từ tối thiểu mong đợi không được dưới 0");
        }
        if (taskRelevance == null || taskRelevance.compareTo(BigDecimal.ZERO) < 0 || taskRelevance.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Relevance của task phải nằm trong khoảng 0 đến 1");
        }
        if (offTopicRatio == null || offTopicRatio.compareTo(BigDecimal.ZERO) < 0 || offTopicRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Ratio của off topic bắt buộc phải từ 0 đến 1");
        }
        if (codeSwitchingRatio == null || codeSwitchingRatio.compareTo(BigDecimal.ZERO) < 0 || codeSwitchingRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Ratio của code switching bắt buộc phải từ 0 đến 1");
        }
        if (asrConfidence == null || asrConfidence.compareTo(BigDecimal.ZERO) < 0 || asrConfidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence của asr bắt buộc phải từ 0 đến 1");
        }
        if (aiConfidence == null || aiConfidence.compareTo(BigDecimal.ZERO) < 0 || aiConfidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence của AI bắt buộc phải từ 0 đến 1");
        }
        if (audioQuality == null || audioQuality.compareTo(BigDecimal.ZERO) < 0 || audioQuality.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Audio Quality bắt buộc phải từ 0 đến 1");
        }
        if (silenceRatio == null || silenceRatio.compareTo(BigDecimal.ZERO) < 0 || silenceRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Ratio của silence bắt buộc phải từ 0 đến 1");
        }
        if (speechRate == null || speechRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Speech rate không được dưới 0");
        }
    }
}
