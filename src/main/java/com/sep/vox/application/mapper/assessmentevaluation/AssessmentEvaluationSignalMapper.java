package com.sep.vox.application.mapper.assessmentevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.sep.vox.application.port.input.command.assessmentevaluation.ConfidenceCaseSignalsInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.EvaluationSignalsInput;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;
import com.sep.vox.domain.valueobject.EvaluationSignals;

public final class AssessmentEvaluationSignalMapper {

    private AssessmentEvaluationSignalMapper() {
    }

    public static EvaluationSignals toDomain(EvaluationSignalsInput dto) {
        if (dto == null) {
            return emptySignals();
        }
        return fromInput(dto);
    }

    private static EvaluationSignals emptySignals() {
        return new EvaluationSignals(
            0,
            0,
            null,
            null,
            null,
            clamp01(null),
            clamp01(null),
            clamp01(null),
            clamp01(null),
            clamp01(null),
            clamp01(null),
            clampNonNegative(null),
            "SUFFICIENT",
            List.of(),
            "NONE",
            null,
            "UNKNOWN",
            List.of(),
            null
        );
    }

    private static EvaluationSignals fromInput(EvaluationSignalsInput dto) {
        return new EvaluationSignals(
            dto.durationSeconds() == null ? 0 : dto.durationSeconds(),
            dto.wordCount() == null ? 0 : dto.wordCount(),
            dto.sentenceCount(),
            dto.lengthRatio() == null ? null : BigDecimal.valueOf(dto.lengthRatio()).setScale(2, RoundingMode.HALF_UP),
            dto.expectedMinWords(),
            clamp01(dto.topicRelevanceScore()),
            clamp01(dto.offTopicRatio()),
            clamp01(dto.codeSwitchingRatio()),
            clamp01(dto.asrConfidenceAvg()),
            clamp01(dto.audioQuality()),
            clamp01(dto.silenceRatio()),
            clampNonNegative(dto.speechRate()),
            dto.evidenceStatus(),
            dto.evidenceReasonCodes(),
            "NONE",
            null,
            "UNKNOWN",
            List.of(),
            toConfidenceCase(dto.confidenceCase())
        );
    }

    private static ConfidenceCaseSignals toConfidenceCase(ConfidenceCaseSignalsInput dto) {
        if (dto == null) {
            return null;
        }
        return new ConfidenceCaseSignals(
            clampNullable01(dto.cAsrLog()),
            clampNullable01(dto.qSnr()),
            clampNullable01(dto.qSpeech()),
            clampNullable01(dto.clippingRatio()),
            clampNullable01(dto.cRef()),
            clampNullable01(dto.cAlign()),
            clampNullable01(dto.cAlignAccuracy()),
            clampNullable01(dto.cAlignCoverage()),
            clampNullable01(dto.cAlignTiming()),
            clampNullable01(dto.cPfBranch()),
            clampNullable01(dto.cGrammar()),
            clampNullable01(dto.cVocabulary()),
            clampNullable01(dto.cCoherence()),
            clampNonNegative(dto.grammarScoreDelta()),
            clampNonNegative(dto.vocabularyScoreDelta()),
            clampNonNegative(dto.coherenceScoreDelta())
        );
    }

    // "null" (not measured yet -- e.g. asrConfidenceAvg when using mai-transcribe-1, which has no logprob)
    // is COMPLETELY DIFFERENT from "0.0" (measured, the worst result). These two functions used to force null -> 0.00, which made the frontend
    // show "Audio quality: 0%" for EVERY turn without real data, even for perfectly
    // normal audio -- keep null as it is here, all the way to the signals JSON; the frontend already handles
    // number | null properly (its result page uses formatConfidencePercent).
    private static BigDecimal clamp01(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0D, Math.min(1D, value))).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal clampNonNegative(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0D, value)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal clampNullable01(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0D, Math.min(1D, value))).setScale(4, RoundingMode.HALF_UP);
    }
}
