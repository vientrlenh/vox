package com.sep.vox.interfaces.kafka.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sep.vox.application.port.input.command.assessmentevaluation.ConfidenceCaseSignalsInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.EvaluationSignalsInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.PhonemeFeedbackInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.PronunciationOverallInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.RecordAssessmentAttemptEvaluationCommand;
import com.sep.vox.application.port.input.command.assessmentevaluation.RecordAssessmentAttemptEvaluationPayloadInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.TurnDetailInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.ValidityResultInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.WordFeedbackInput;
import com.sep.vox.interfaces.kafka.dto.ConfidenceCaseSignalsDto;
import com.sep.vox.interfaces.kafka.dto.CriterionScoreDto;
import com.sep.vox.interfaces.kafka.dto.EvaluationSignalsDto;
import com.sep.vox.interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedPayloadDto;
import com.sep.vox.interfaces.kafka.dto.PronunciationOverallDto;
import com.sep.vox.interfaces.kafka.dto.TurnDetailDto;
import com.sep.vox.interfaces.kafka.dto.ValidityResultDto;

/**
 * Converts {@code AssessmentAttemptEvaluationCompletedEventDto} (the Kafka-specific wire format, read directly
 * by Jackson) into {@code RecordAssessmentAttemptEvaluationCommand} (application layer) --
 * the application/domain layers must not know the Kafka DTO format, so this mapping MUST happen in the interfaces
 * layer before calling the use case.
 */
public final class RecordAssessmentAttemptEvaluationCommandMapper {

    private RecordAssessmentAttemptEvaluationCommandMapper() {
    }

    public static RecordAssessmentAttemptEvaluationCommand toCommand(AssessmentAttemptEvaluationCompletedEventDto dto) {
        if (dto == null) {
            return null;
        }
        return new RecordAssessmentAttemptEvaluationCommand(
            dto.eventType(),
            dto.schemaVersion(),
            dto.assessmentAttemptId(),
            dto.answerId(),
            dto.questionId(),
            toPayload(dto.payload())
        );
    }

    private static RecordAssessmentAttemptEvaluationPayloadInput toPayload(AssessmentAttemptEvaluationCompletedPayloadDto payload) {
        if (payload == null) {
            return null;
        }
        return new RecordAssessmentAttemptEvaluationPayloadInput(
            toTurns(payload.turns()),
            toCriteria(payload.criteria()),
            toSignals(payload.signals()),
            toValidity(payload.validity()),
            payload.feedbackSummary(),
            toObjectList(payload.suggestions()),
            payload.modelVersion(),
            payload.promptVersion(),
            payload.evaluatedAt()
        );
    }

    // Package-private (no longer private) so RecordPracticeAttemptEvaluationCommandMapper can reuse
    // it: Python grades practice with EXACTLY the same graph as assessment grading so the payload has the same
    // shape, and copying it into a second version invites the two copies to drift apart.
    static Map<String, CriterionScoreInput> toCriteria(Map<String, CriterionScoreDto> criteria) {
        if (criteria == null) {
            return null;
        }
        return criteria.entrySet().stream().collect(Collectors.toMap(
            k -> k.getKey(),
            entry -> toCriterion(entry.getValue())
        ));
    }

    private static CriterionScoreInput toCriterion(CriterionScoreDto dto) {
        if (dto == null) {
            return null;
        }
        return new CriterionScoreInput(
            dto.score(),
            dto.level(),
            dto.status(),
            dto.source(),
            dto.subscores(),
            dto.note(),
            dto.suggestion(),
            dto.weaknessLabels(),
            dto.evidenceSpans(),
            dto.recommendationTag(),
            dto.matchedBandCode()
        );
    }

    // Package-private (no longer private) so RecordPracticeAttemptEvaluationCommandMapper can reuse
    // it: Python grades practice with EXACTLY the same graph as assessment grading so the payload has the same
    // shape, and copying it into a second version invites the two copies to drift apart.
    static EvaluationSignalsInput toSignals(EvaluationSignalsDto dto) {
        if (dto == null) {
            return null;
        }
        return new EvaluationSignalsInput(
            dto.durationSeconds(),
            dto.wordCount(),
            dto.sentenceCount(),
            dto.lengthRatio(),
            dto.expectedMinWords(),
            dto.asrConfidenceAvg(),
            dto.topicRelevanceScore(),
            dto.offTopicRatio(),
            dto.codeSwitchingRatio(),
            dto.speechRate(),
            dto.audioQuality(),
            dto.silenceRatio(),
            dto.evidenceStatus(),
            dto.evidenceReasonCodes(),
            toConfidenceCase(dto.confidenceCase())
        );
    }

    private static ConfidenceCaseSignalsInput toConfidenceCase(ConfidenceCaseSignalsDto dto) {
        if (dto == null) {
            return null;
        }
        return new ConfidenceCaseSignalsInput(
            dto.cAsrLog(),
            dto.qSnr(),
            dto.qSpeech(),
            dto.clippingRatio(),
            dto.cRef(),
            dto.cAlign(),
            dto.cAlignAccuracy(),
            dto.cAlignCoverage(),
            dto.cAlignTiming(),
            dto.cPfBranch(),
            dto.cGrammar(),
            dto.cVocabulary(),
            dto.cCoherence(),
            dto.grammarScoreDelta(),
            dto.vocabularyScoreDelta(),
            dto.coherenceScoreDelta()
        );
    }

    private static ValidityResultInput toValidity(ValidityResultDto dto) {
        if (dto == null) {
            return null;
        }
        return new ValidityResultInput(
            dto.validForScoring(),
            dto.action(),
            dto.overallSeverity(),
            toObjectList(dto.ruleResults()),
            toObjectList(dto.flags()),
            dto.scoreCaps(),
            toObjectList(dto.penalties()),
            dto.notes(),
            dto.transcriptSource(),
            dto.transcriptWordCount()
        );
    }

    // Package-private (no longer private) so RecordPracticeAttemptEvaluationCommandMapper can reuse
    // it: Python grades practice with EXACTLY the same graph as assessment grading so the payload has the same
    // shape, and copying it into a second version invites the two copies to drift apart.
    static List<TurnDetailInput> toTurns(List<TurnDetailDto> turns) {
        if (turns == null) {
            return null;
        }
        return turns.stream().map(RecordAssessmentAttemptEvaluationCommandMapper::toTurn).toList();
    }

    private static TurnDetailInput toTurn(TurnDetailDto dto) {
        if (dto == null) {
            return null;
        }
        return new TurnDetailInput(
            dto.turnId(),
            dto.turnOrder(),
            dto.turnType(),
            dto.promptText(),
            dto.audioUrl(),
            dto.transcript(),
            dto.wordCount(),
            dto.durationSeconds(),
            dto.asrConfidence(),
            toPronunciationOverall(dto.pronunciationOverall()),
            toWordFeedback(dto.wordFeedback())
        );
    }

    private static List<WordFeedbackInput> toWordFeedback(List<tools.jackson.databind.JsonNode> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
            .filter(value -> value != null && value.isObject())
            .map(value -> new WordFeedbackInput(
                textOrNull(value.get("word")),
                doubleOrNull(value.get("accuracyScore")),
                doubleOrNull(value.get("effectiveScore")),
                textOrNull(value.get("errorType")),
                textOrNull(value.get("color")),
                textOrNull(value.get("level")),
                textOrNull(value.get("errorNote")),
                booleanOrNull(value.get("hasCriticalIssue")),
                toPhonemes(value.get("phonemes"))
            ))
            .toList();
    }

    private static List<PhonemeFeedbackInput> toPhonemes(tools.jackson.databind.JsonNode values) {
        if (values == null || !values.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
            .filter(value -> value != null && value.isObject())
            .map(value -> new PhonemeFeedbackInput(
                textOrNull(value.get("phoneme")),
                doubleOrNull(value.get("accuracyScore")),
                textOrNull(value.get("color")),
                textOrNull(value.get("level")),
                textOrNull(value.get("note"))
            ))
            .toList();
    }

    private static String textOrNull(tools.jackson.databind.JsonNode value) {
        return value == null || value.isNull() ? null : value.asString();
    }

    private static Double doubleOrNull(tools.jackson.databind.JsonNode value) {
        return value == null || !value.isNumber() ? null : value.asDouble();
    }

    private static Boolean booleanOrNull(tools.jackson.databind.JsonNode value) {
        return value == null || !value.isBoolean() ? null : value.asBoolean();
    }

    private static PronunciationOverallInput toPronunciationOverall(PronunciationOverallDto dto) {
        if (dto == null) {
            return null;
        }
        return new PronunciationOverallInput(
            dto.accuracyScore(),
            dto.fluencyScore(),
            dto.prosodyScore(),
            dto.pronScore(),
            dto.completenessScore()
        );
    }

    private static <T> List<Object> toObjectList(List<T> values) {
        return values == null ? null : List.copyOf(values);
    }
}
