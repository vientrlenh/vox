package com.sep.vox.application.mapper.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.sep.vox.domain.valueobject.EvaluationSignals;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto;

public final class ExamEvaluationSignalMapper {

    private ExamEvaluationSignalMapper() {
    }

    public static EvaluationSignals toDomain(ExamAttemptEvaluationCompletedEventDto.EvaluationSignalsDto dto) {
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
                clamp01(null),
                clampNonNegative(null)
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
            clamp01(dto.aiConfidence()),
            clamp01(dto.audioQuality()),
            clamp01(dto.silenceRatio()),
            clampNonNegative(dto.speechRate())
        );
    }

    private static BigDecimal clamp01(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(Math.max(0D, Math.min(1D, value))).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal clampNonNegative(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(Math.max(0D, value)).setScale(2, RoundingMode.HALF_UP);
    }
}
