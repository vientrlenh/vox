package com.sep.vox.application.mapper.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.sep.vox.application.port.input.command.examevaluation.ConfidenceCaseSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.EvaluationSignalsInput;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;
import com.sep.vox.domain.valueobject.EvaluationSignals;

public final class ExamEvaluationSignalMapper {

    private ExamEvaluationSignalMapper() {
    }

    public static EvaluationSignals toDomain(EvaluationSignalsInput dto) {
        if (dto == null) {
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
                java.util.List.of(),
                "NONE",
                null,
                "UNKNOWN",
                java.util.List.of(),
                null
            );
        }

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
            java.util.List.of(),
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
            clampNullable01(dto.cDiscourse()),
            clampNonNegative(dto.grammarScoreDelta()),
            clampNonNegative(dto.vocabularyScoreDelta()),
            clampNonNegative(dto.discourseScoreDelta())
        );
    }

    // "null" (chưa đo được -- VD asrConfidenceAvg khi dùng mai-transcribe-1, không có logprob)
    // KHÁC HẲN "0.0" (đã đo, kết quả tệ nhất). Trước đây 2 hàm này ép null -> 0.00, khiến frontend
    // hiện "Audio quality: 0%" cho MỌI turn không có dữ liệu thật, kể cả audio hoàn toàn bình
    // thường -- giữ nguyên null ở đây, để nguyên tới tận signals JSON, frontend đã tự xử lý
    // number | null đúng cách rồi (ExamResultPages.tsx dùng formatConfidencePercent).
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
