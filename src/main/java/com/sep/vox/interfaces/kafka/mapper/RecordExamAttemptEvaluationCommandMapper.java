package com.sep.vox.interfaces.kafka.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sep.vox.application.port.input.command.examevaluation.ConfidenceCaseSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.examevaluation.EvaluationSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.PronunciationOverallInput;
import com.sep.vox.application.port.input.command.examevaluation.RecordExamAttemptEvaluationCommand;
import com.sep.vox.application.port.input.command.examevaluation.RecordExamAttemptEvaluationPayloadInput;
import com.sep.vox.application.port.input.command.examevaluation.TurnDetailInput;
import com.sep.vox.application.port.input.command.examevaluation.ValidityResultInput;
import com.sep.vox.interfaces.kafka.dto.ConfidenceCaseSignalsDto;
import com.sep.vox.interfaces.kafka.dto.CriterionScoreDto;
import com.sep.vox.interfaces.kafka.dto.EvaluationSignalsDto;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedPayloadDto;
import com.sep.vox.interfaces.kafka.dto.PronunciationOverallDto;
import com.sep.vox.interfaces.kafka.dto.TurnDetailDto;
import com.sep.vox.interfaces.kafka.dto.ValidityResultDto;

/**
 * Chuyển {@code ExamAttemptEvaluationCompletedEventDto} (wire format riêng của Kafka, đọc trực
 * tiếp bằng Jackson) sang {@code RecordExamAttemptEvaluationCommand} (application layer) --
 * application/domain không được biết tới định dạng Kafka DTO, mapping này PHẢI xảy ra ở interfaces
 * layer trước khi gọi use case.
 */
public final class RecordExamAttemptEvaluationCommandMapper {

    private RecordExamAttemptEvaluationCommandMapper() {
    }

    public static RecordExamAttemptEvaluationCommand toCommand(ExamAttemptEvaluationCompletedEventDto dto) {
        if (dto == null) {
            return null;
        }
        return new RecordExamAttemptEvaluationCommand(
            dto.eventType(),
            dto.schemaVersion(),
            dto.examAttemptId(),
            dto.answerId(),
            dto.questionId(),
            toPayload(dto.payload())
        );
    }

    private static RecordExamAttemptEvaluationPayloadInput toPayload(ExamAttemptEvaluationCompletedPayloadDto payload) {
        if (payload == null) {
            return null;
        }
        return new RecordExamAttemptEvaluationPayloadInput(
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

    private static Map<String, CriterionScoreInput> toCriteria(Map<String, CriterionScoreDto> criteria) {
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
            dto.suggestion()
        );
    }

    private static EvaluationSignalsInput toSignals(EvaluationSignalsDto dto) {
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
            dto.cDiscourse(),
            dto.grammarScoreDelta(),
            dto.vocabularyScoreDelta(),
            dto.discourseScoreDelta()
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

    private static List<TurnDetailInput> toTurns(List<TurnDetailDto> turns) {
        if (turns == null) {
            return null;
        }
        return turns.stream().map(RecordExamAttemptEvaluationCommandMapper::toTurn).toList();
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
            toObjectList(dto.wordFeedback())
        );
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
