package com.sep.vox.interfaces.kafka.mapper;

import com.sep.vox.interfaces.kafka.dto.EvaluationSignalsDto;
import com.sep.vox.interfaces.kafka.dto.PracticeAttemptEvaluationCompletedPayloadDto;
import com.sep.vox.interfaces.kafka.dto.ValidityResultDto;
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
        PracticeAttemptEvaluationCompletedPayloadDto payload = dto.payload();
        ValidityResultDto validity = payload == null ? null : payload.validity();
        EvaluationSignalsDto signals = payload == null ? null : payload.signals();
        return new RecordPracticeAttemptEvaluationCommand(
            UUID.fromString(dto.practiceResponseId()),
            validity == null || validity.validForScoring() == null || validity.validForScoring(),
            toConfidenceCase(signals == null ? null : signals.confidenceCase()),
            signals == null ? null : decimalOrNull(signals.audioQuality()),
            signals == null ? null : decimalOrNull(signals.codeSwitchingRatio()),
            signals == null || signals.wordCount() == null ? 0 : signals.wordCount(),
            toCriterionScores(payload),
            payload == null ? null : payload.evaluatedAt()
            // rawCriteria/turns/signals are NO LONGER mapped into the command. Those three fields only served
            // deriving weak-point observations (removed) -- the use case does not read them, so keeping them
            // would build three structures on every grading for nobody to use.
            //
            // The Kafka wire DTO STAYS UNCHANGED: Python still emits all fields as before, Java just ignores
            // them. Not touching the payload shape means nothing has to change on the grading side.
        );
    }

    private static List<PracticeCriterionScoreInput> toCriterionScores(PracticeAttemptEvaluationCompletedPayloadDto payload) {
        if (payload == null || payload.criteria() == null) {
            return List.of();
        }
        return payload.criteria().entrySet().stream()
            // Ignore matchedBandCode even if Python sends it: practice grading compares against EXACTLY the
            // one band the student chose, so only the score within that band is needed, not a
            // classification.
            .map(entry -> new PracticeCriterionScoreInput(
                entry.getKey(),
                entry.getValue() == null ? null : entry.getValue().score()
            ))
            .toList();
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
