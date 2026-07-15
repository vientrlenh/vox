package com.sep.vox.application.port.input.usecase.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.examevaluation.ExamEvaluationSignalMapper;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamItemEvaluationTurn;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto;

import tools.jackson.databind.json.JsonMapper;

@Service
public class RecordExamAttemptEvaluationUseCase implements IUseCase<ExamAttemptEvaluationCompletedEventDto, Void> {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate phaseOneTransactionTemplate;
    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.50).setScale(2, RoundingMode.HALF_UP);

    public RecordExamAttemptEvaluationUseCase(
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            PlatformTransactionManager transactionManager,
            JsonMapper jsonMapper) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examItemEvaluationTurnRepository = examItemEvaluationTurnRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.jsonMapper = jsonMapper;
        this.phaseOneTransactionTemplate = new TransactionTemplate(transactionManager);
        this.phaseOneTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Void execute(ExamAttemptEvaluationCompletedEventDto input) {
        var persisted = persistEvaluation(input);

        if (allResponsesHaveEvaluations(persisted.sessionId())) {
            upsertExamCandidateResultUseCase.execute(persisted.sessionId());
            updateExamSessionStatusUseCase.execute(new UpdateExamSessionStatusCommand(
                persisted.sessionId(),
                ExamSessionStatus.GRADED
            ));
        }
        return null;
    }

    private PersistedEvaluation persistEvaluation(ExamAttemptEvaluationCompletedEventDto input) {
        var persisted = phaseOneTransactionTemplate.execute(status -> {
            var responseId = UUID.fromString(input.answerId());
            var response = examItemResponseRepository.findById(responseId)
                .orElseThrow(() -> new NotFoundException("không thể tìm thấy câu trả lời cho evaluation"));

            var criteriaMap = input.payload() == null || input.payload().criteria() == null
                ? Map.<String, ExamAttemptEvaluationCompletedEventDto.CriterionScoreDto>of()
                : input.payload().criteria();

            // Resolved before computing itemScore (not after, like before this fix) --
            // the rubric's own per-criterion weight (e.g. pronunciation .25, fluency
            // .20...) must be applied first to combine the 5 criteria into one item
            // score; ExamPaperItem.weight (question weight within the paper) is a
            // separate, later step applied on top of this item score by
            // ExamSessionResultCalculator when rolling up section/total scores.
            var rubricCriteriaByCode = rubricCriterionRepository.findByRubricVersionId(
                resolveRubricVersionId(response.getSessionId())
            ).stream().collect(Collectors.toMap(
                item -> normalizeCode(item.getCode()),
                Function.identity(),
                (left, right) -> left
            ));
            BigDecimal itemScore = computeWeightedItemScore(criteriaMap, rubricCriteriaByCode);

            var validity = input.payload() == null ? null : input.payload().validity();
            var signals = ExamEvaluationSignalMapper.toDomain(input.payload() == null ? null : input.payload().signals());
            var overallConfidence = clampUnit(averageConfidence(
                input.payload() == null ? null : input.payload().signals()));
            var hasCriticalValidityFlag = hasCriticalValidityFlag(validity);
            boolean requiresHumanReview = hasCriticalValidityFlag || overallConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0;
            String reviewReasonCode = hasCriticalValidityFlag
                ? "VALIDITY_FLAGGED"
                : (overallConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0 ? "LOW_CONFIDENCE" : null);
            var requiresRetake = requiresRetake(validity);
            boolean markedInvalid = validity != null && Boolean.FALSE.equals(validity.validForScoring());
            if ("uncooperative_move_on".equals(response.getTerminationReason())) {
                itemScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                requiresHumanReview = true;
                reviewReasonCode = "CONDUCT_VIOLATION";
                markedInvalid = true;
            }

            var existingEvaluation = examItemEvaluationRepository.findLatestByResponseId(response.getId()).orElse(null);
            var evaluation = existingEvaluation == null
                ? new ExamItemEvaluation(
                    response.getId(),
                    response.getPaperItemId(),
                    ExamEvaluationEngineType.AI_SINGLE,
                    input.payload() == null ? "gpt-4o" : input.payload().modelVersion(),
                    1,
                    null,
                    itemScore,
                    itemScore,
                    overallConfidence,
                    requiresHumanReview,
                    reviewReasonCode,
                    markedInvalid,
                    requiresRetake,
                    signals,
                    toNullableJson(validity),
                    input.payload() == null ? "" : input.payload().feedbackSummary(),
                    toJson(input.payload() == null ? List.of() : input.payload().suggestions()),
                    input.payload() == null ? null : input.payload().promptVersion(),
                    ExamItemEvaluationStatus.AUTO_GRADED,
                    parseEvaluatedAt(input.payload() == null ? null : input.payload().evaluatedAt())
                )
                : existingEvaluation;

            if (existingEvaluation != null) {
                evaluation.setResponseId(response.getId());
                evaluation.setPaperItemId(response.getPaperItemId());
                evaluation.setEngineType(ExamEvaluationEngineType.AI_SINGLE);
                evaluation.setGradedByModel(input.payload() == null ? "gpt-4o" : input.payload().modelVersion());
                evaluation.setSampleCount(1);
                evaluation.setReviewerId(null);
                evaluation.setRawItemScore(itemScore);
                evaluation.setItemScore(itemScore);
                evaluation.setOverallConfidence(overallConfidence);
                evaluation.setRequiresHumanReview(requiresHumanReview);
                evaluation.setReviewReasonCode(reviewReasonCode);
                evaluation.setMarkedInvalid(markedInvalid);
                evaluation.setRequiresRetake(requiresRetake);
                evaluation.setSignals(signals);
                evaluation.setValidityJson(toNullableJson(validity));
                evaluation.setFeedbackSummary(input.payload() == null ? "" : input.payload().feedbackSummary());
                evaluation.setSuggestionsJson(toJson(input.payload() == null ? List.of() : input.payload().suggestions()));
                evaluation.setPromptVersion(input.payload() == null ? null : input.payload().promptVersion());
                evaluation.setStatus(ExamItemEvaluationStatus.AUTO_GRADED);
                evaluation.setEvaluatedAt(parseEvaluatedAt(input.payload() == null ? null : input.payload().evaluatedAt()));
            }

            var savedEvaluation = examItemEvaluationRepository.save(evaluation);
            if (existingEvaluation != null) {
                examItemCriterionScoreRepository.deleteByEvaluationIdIn(List.of(savedEvaluation.getId()));
                examItemEvaluationTurnRepository.deleteByEvaluationIdIn(List.of(savedEvaluation.getId()));
            }

            var criterionScores = criteriaMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().score() != null)
                .map(entry -> {
                    var criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
                    if (criterion == null) {
                        return null;
                    }
                    var score = BigDecimal.valueOf(entry.getValue().score()).setScale(2, RoundingMode.HALF_UP);
                    return new ExamItemCriterionScore(
                        savedEvaluation.getId(),
                        criterion.getId(),
                        score,
                        score,
                        entry.getValue().note()
                    );
                })
                .filter(item -> item != null)
                .toList();
            if (!criterionScores.isEmpty()) {
                examItemCriterionScoreRepository.saveAll(criterionScores);
            }

            var turns = input.payload() == null || input.payload().turns() == null
                ? List.<ExamItemEvaluationTurn>of()
                : input.payload().turns().stream()
                    .map(turn -> new ExamItemEvaluationTurn(
                        UUID.randomUUID(),
                        savedEvaluation.getId(),
                        turn.turnOrder() == null ? 0 : turn.turnOrder(),
                        parseTurnType(turn.turnType()),
                        turn.promptText(),
                        turn.audioUrl(),
                        turn.transcript() == null ? "" : turn.transcript(),
                        turn.wordCount() == null ? 0 : turn.wordCount(),
                        turn.durationSeconds(),
                        turn.asrConfidence(),
                        toJson(turn.pronunciationOverall()),
                        toJson(turn.wordFeedback())
                    ))
                    .toList();
            if (!turns.isEmpty()) {
                examItemEvaluationTurnRepository.saveAll(turns);
            }

            return new PersistedEvaluation(response.getSessionId());
        });

        if (persisted == null) {
            throw new IllegalStateException("Không thể lưu kết quả chấm cho câu trả lời");
        }
        return persisted;
    }

    private record PersistedEvaluation(UUID sessionId) {
    }

    private BigDecimal computeWeightedItemScore(
            Map<String, ExamAttemptEvaluationCompletedEventDto.CriterionScoreDto> criteriaMap,
            Map<String, RubricCriterion> rubricCriteriaByCode) {
        var weightedSum = BigDecimal.ZERO;
        var weightSum = BigDecimal.ZERO;
        var unweightedSum = BigDecimal.ZERO;
        var unweightedCount = 0;

        for (var entry : criteriaMap.entrySet()) {
            var criterionScore = entry.getValue();
            if (criterionScore == null || criterionScore.score() == null) {
                continue;
            }
            var score = BigDecimal.valueOf(criterionScore.score());
            unweightedSum = unweightedSum.add(score);
            unweightedCount++;

            var criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
            if (criterion == null || criterion.getWeight() == null) {
                continue;
            }
            weightedSum = weightedSum.add(score.multiply(criterion.getWeight()));
            weightSum = weightSum.add(criterion.getWeight());
        }

        if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
            return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
        }

        // Fallback: none of the incoming criteria matched a RubricCriterion with
        // a real weight (e.g. codes don't line up with what's seeded for this
        // rubric version) -- plain mean, same behavior as before this fix, so
        // grading doesn't silently collapse to zero.
        return unweightedCount == 0
            ? BigDecimal.ZERO
            : unweightedSum.divide(BigDecimal.valueOf(unweightedCount), 2, RoundingMode.HALF_UP);
    }

    private UUID resolveRubricVersionId(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy phiên thi cho evaluation"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra cho evaluation"));
        if (exam.getAssessmentPolicyId() == null) {
            throw new NotFoundException("bài kiểm tra chưa gắn assessment policy");
        }
        var policy = assessmentPolicyRepository.findById(exam.getAssessmentPolicyId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy assessment policy cho bài kiểm tra"));
        return policy.getRubricVersionId();
    }

    private boolean allResponsesHaveEvaluations(UUID sessionId) {
        return examItemResponseRepository.findBySessionId(sessionId).stream()
            .allMatch(item -> examItemEvaluationRepository.findLatestByResponseId(item.getId()).isPresent());
    }

    private OffsetDateTime parseEvaluatedAt(String value) {
        return value == null || value.isBlank() ? OffsetDateTime.now() : OffsetDateTime.parse(value);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private TurnType parseTurnType(String value) {
        if (value == null || value.isBlank()) {
            return TurnType.MAIN;
        }
        try {
            return TurnType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TurnType.MAIN;
        }
    }

    private Double averageConfidence(ExamAttemptEvaluationCompletedEventDto.EvaluationSignalsDto signals) {
        if (signals == null) {
            return null;
        }
        var aiConfidence = signals.aiConfidence();
        var asrConfidence = signals.asrConfidenceAvg();
        if (aiConfidence == null && asrConfidence == null) {
            return null;
        }
        if (aiConfidence == null) {
            return asrConfidence;
        }
        if (asrConfidence == null) {
            return aiConfidence;
        }
        return (aiConfidence + asrConfidence) / 2;
    }

    private BigDecimal clampUnit(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        var decimal = BigDecimal.valueOf(Math.max(0D, Math.min(1D, value)));
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasCriticalValidityFlag(ExamAttemptEvaluationCompletedEventDto.ValidityResultDto validity) {
        return validity != null
            && validity.overallSeverity() != null
            && "critical".equalsIgnoreCase(validity.overallSeverity());
    }

    private boolean requiresRetake(ExamAttemptEvaluationCompletedEventDto.ValidityResultDto validity) {
        return validity != null
            && validity.action() != null
            && "reject_or_zero".equalsIgnoreCase(validity.action());
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("không thể serialize dữ liệu evaluation", ex);
        }
    }

    private String toNullableJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("không thể serialize dữ liệu evaluation", ex);
        }
    }
}
