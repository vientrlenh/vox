package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;
import java.util.List;

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
    String evidenceStatus,
    List<String> evidenceReasonCodes,
    String uncertaintyType,
    String confidenceMode,
    String audioGateStatus,
    List<String> audioGateReasonCodes,
    ConfidenceCaseSignals confidenceCase
) {
    public EvaluationSignals {
        evidenceStatus = evidenceStatus == null ? "SUFFICIENT" : evidenceStatus;
        evidenceReasonCodes = evidenceReasonCodes == null ? List.of() : List.copyOf(evidenceReasonCodes);
        uncertaintyType = uncertaintyType == null ? "NONE" : uncertaintyType;
        audioGateReasonCodes = audioGateReasonCodes == null ? List.of() : List.copyOf(audioGateReasonCodes);
        requireValidCounts(durationSeconds, wordCount, sentenceCount, lengthRatio, expectedMinWords);
        requireValidMeasuredSignals(
            taskRelevance, offTopicRatio, codeSwitchingRatio, asrConfidence, audioQuality, silenceRatio, speechRate);
    }

    private static void requireValidCounts(
            int durationSeconds, int wordCount, Integer sentenceCount, BigDecimal lengthRatio,
            Integer expectedMinWords) {
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("Thời gian không được dưới 0");
        }
        if (wordCount < 0) {
            throw new IllegalArgumentException("Số từ không được dưới 0");
        }
        if (sentenceCount != null && sentenceCount < 0) {
            throw new IllegalArgumentException("Số câu không được dưới 0");
        }
        requireNotNegative(lengthRatio, "Length ratio không được dưới 0");
        if (expectedMinWords != null && expectedMinWords < 0) {
            throw new IllegalArgumentException("Số từ tối thiểu mong đợi không được dưới 0");
        }
    }

    // The signals below are BigDecimal and may be null when that signal has NOT BEEN MEASURED (e.g.
    // asrConfidence is null when using an ASR that returns no logprob) -- null is DIFFERENT from 0 (measured, the worst),
    // so the range is only validated when there is a real value, and non-null is no longer forced as before (which once made
    // callers fake 0.0 for "not measured", incorrectly appearing as "audio quality 0%" in the
    // frontend -- see AssessmentEvaluationSignalMapper.clamp01).
    private static void requireValidMeasuredSignals(
            BigDecimal taskRelevance, BigDecimal offTopicRatio, BigDecimal codeSwitchingRatio,
            BigDecimal asrConfidence, BigDecimal audioQuality, BigDecimal silenceRatio, BigDecimal speechRate) {
        requireWithinUnitRange(taskRelevance, "Relevance của task phải nằm trong khoảng 0 đến 1");
        requireWithinUnitRange(offTopicRatio, "Ratio của off topic bắt buộc phải từ 0 đến 1");
        requireWithinUnitRange(codeSwitchingRatio, "Ratio của code switching bắt buộc phải từ 0 đến 1");
        requireWithinUnitRange(asrConfidence, "Confidence của asr bắt buộc phải từ 0 đến 1");
        requireWithinUnitRange(audioQuality, "Audio Quality bắt buộc phải từ 0 đến 1");
        requireWithinUnitRange(silenceRatio, "Ratio của silence bắt buộc phải từ 0 đến 1");
        requireNotNegative(speechRate, "Speech rate không được dưới 0");
    }

    private static void requireNotNegative(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireWithinUnitRange(BigDecimal value, String message) {
        if (value != null && (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException(message);
        }
    }

    public EvaluationSignals withAssessment(
            String resolvedUncertaintyType, String resolvedConfidenceMode, String resolvedAudioGateStatus,
            List<String> resolvedAudioGateReasonCodes) {
        return new EvaluationSignals(
            durationSeconds, wordCount, sentenceCount, lengthRatio, expectedMinWords,
            taskRelevance, offTopicRatio, codeSwitchingRatio, asrConfidence, audioQuality,
            silenceRatio, speechRate, evidenceStatus, evidenceReasonCodes,
            resolvedUncertaintyType, resolvedConfidenceMode, resolvedAudioGateStatus,
            resolvedAudioGateReasonCodes, confidenceCase
        );
    }
}
