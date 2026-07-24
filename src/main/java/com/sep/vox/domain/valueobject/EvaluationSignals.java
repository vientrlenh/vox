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
    BigDecimal audioQuality,
    BigDecimal silenceRatio,
    BigDecimal speechRate,
    ConfidenceCaseSignals confidenceCase
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
        // Các field bên dưới là BigDecimal có thể null khi tín hiệu đó CHƯA ĐO ĐƯỢC (VD
        // asrConfidence null khi dùng ASR không trả logprob) -- null KHÁC 0 (đã đo, tệ nhất),
        // nên chỉ validate range khi có giá trị thật, không ép non-null như trước (từng khiến
        // caller phải giả 0.0 cho "chưa đo được", hiện ra sai thành "audio quality 0%" ở
        // frontend -- xem ExamEvaluationSignalMapper.clamp01).
        if (taskRelevance != null && (taskRelevance.compareTo(BigDecimal.ZERO) < 0 || taskRelevance.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Relevance của task phải nằm trong khoảng 0 đến 1");
        }
        if (offTopicRatio != null && (offTopicRatio.compareTo(BigDecimal.ZERO) < 0 || offTopicRatio.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Ratio của off topic bắt buộc phải từ 0 đến 1");
        }
        if (codeSwitchingRatio != null && (codeSwitchingRatio.compareTo(BigDecimal.ZERO) < 0 || codeSwitchingRatio.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Ratio của code switching bắt buộc phải từ 0 đến 1");
        }
        if (asrConfidence != null && (asrConfidence.compareTo(BigDecimal.ZERO) < 0 || asrConfidence.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Confidence của asr bắt buộc phải từ 0 đến 1");
        }
        if (audioQuality != null && (audioQuality.compareTo(BigDecimal.ZERO) < 0 || audioQuality.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Audio Quality bắt buộc phải từ 0 đến 1");
        }
        if (silenceRatio != null && (silenceRatio.compareTo(BigDecimal.ZERO) < 0 || silenceRatio.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Ratio của silence bắt buộc phải từ 0 đến 1");
        }
        if (speechRate != null && speechRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Speech rate không được dưới 0");
        }
    }
}
