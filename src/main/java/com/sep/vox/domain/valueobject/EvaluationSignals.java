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
        // The fields below are BigDecimal and may be null when that signal has NOT BEEN MEASURED (e.g.
        // asrConfidence is null when using an ASR that returns no logprob) -- null is DIFFERENT from 0 (measured, the worst),
        // so the range is only validated when there is a real value, and non-null is no longer forced as before (which once made
        // callers fake 0.0 for "not measured", incorrectly appearing as "audio quality 0%" in the
        // frontend -- see AssessmentEvaluationSignalMapper.clamp01).
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

    public EvaluationSignals withAssessment(
            String resolvedUncertaintyType,
            String resolvedConfidenceMode,
            String resolvedAudioGateStatus,
            List<String> resolvedAudioGateReasonCodes) {
        return new EvaluationSignals(
            durationSeconds,
            wordCount,
            sentenceCount,
            lengthRatio,
            expectedMinWords,
            taskRelevance,
            offTopicRatio,
            codeSwitchingRatio,
            asrConfidence,
            audioQuality,
            silenceRatio,
            speechRate,
            evidenceStatus,
            evidenceReasonCodes,
            resolvedUncertaintyType,
            resolvedConfidenceMode,
            resolvedAudioGateStatus,
            resolvedAudioGateReasonCodes,
            confidenceCase
        );
    }
}
