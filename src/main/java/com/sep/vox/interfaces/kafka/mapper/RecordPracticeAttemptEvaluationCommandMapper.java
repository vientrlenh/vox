package com.sep.vox.interfaces.kafka.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.practiceevaluation.PracticeCriterionScoreInput;
import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;
import com.sep.vox.interfaces.kafka.dto.ConfidenceCaseSignalsDto;
import com.sep.vox.interfaces.kafka.dto.PracticeAttemptEvaluationCompletedEventDto;

/**
 * Chuyển {@code PracticeAttemptEvaluationCompletedEventDto} (wire format riêng của Kafka) sang
 * {@code RecordPracticeAttemptEvaluationCommand} (application layer) -- application/domain không
 * được biết tới định dạng Kafka DTO, mapping này PHẢI xảy ra ở interfaces layer trước khi gọi use case.
 */
public final class RecordPracticeAttemptEvaluationCommandMapper {

    private RecordPracticeAttemptEvaluationCommandMapper() {
    }

    public static RecordPracticeAttemptEvaluationCommand toCommand(PracticeAttemptEvaluationCompletedEventDto dto) {
        var payload = dto.payload();
        var validity = payload == null ? null : payload.validity();
        var signals = payload == null ? null : payload.signals();
        var criteria = payload == null || payload.criteria() == null
            ? List.<PracticeCriterionScoreInput>of()
            : payload.criteria().entrySet().stream()
                .map(entry -> new PracticeCriterionScoreInput(
                    entry.getKey(),
                    entry.getValue() == null ? null : entry.getValue().score(),
                    entry.getValue() == null ? null : entry.getValue().matchedBandCode()
                ))
                .toList();
        return new RecordPracticeAttemptEvaluationCommand(
            UUID.fromString(dto.practiceResponseId()),
            validity == null || validity.validForScoring() == null || validity.validForScoring(),
            toConfidenceCase(signals == null ? null : signals.confidenceCase()),
            signals == null ? null : decimalOrNull(signals.audioQuality()),
            signals == null ? null : decimalOrNull(signals.codeSwitchingRatio()),
            signals == null || signals.wordCount() == null ? 0 : signals.wordCount(),
            criteria,
            payload == null ? null : payload.evaluatedAt(),
            // Dùng lại đúng ba helper của mapper bài thi (cùng package) chứ không viết bản thứ
            // hai: payload hai bên là cùng một hình dạng do Python phát ra từ cùng đồ thị chấm.
            payload == null
                ? null
                : RecordExamAttemptEvaluationCommandMapper.toCriteria(payload.criteria()),
            payload == null
                ? null
                : RecordExamAttemptEvaluationCommandMapper.toTurns(payload.turns()),
            RecordExamAttemptEvaluationCommandMapper.toSignals(signals)
        );
    }

    private static ConfidenceCaseSignals toConfidenceCase(ConfidenceCaseSignalsDto dto) {
        if (dto == null) {
            return null;
        }
        return new ConfidenceCaseSignals(
            decimalOrNull(dto.cAsrLog()),
            decimalOrNull(dto.qSnr()),
            decimalOrNull(dto.qSpeech()),
            decimalOrNull(dto.clippingRatio()),
            decimalOrNull(dto.cRef()),
            decimalOrNull(dto.cAlign()),
            decimalOrNull(dto.cAlignAccuracy()),
            decimalOrNull(dto.cAlignCoverage()),
            decimalOrNull(dto.cAlignTiming()),
            decimalOrNull(dto.cPfBranch()),
            decimalOrNull(dto.cGrammar()),
            decimalOrNull(dto.cVocabulary()),
            decimalOrNull(dto.cCoherence()),
            decimalOrNull(dto.grammarScoreDelta()),
            decimalOrNull(dto.vocabularyScoreDelta()),
            decimalOrNull(dto.coherenceScoreDelta())
        );
    }

    private static BigDecimal decimalOrNull(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
