package com.sep.vox.application.port.input.usecase.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
import com.sep.vox.application.port.input.command.CompleteExamSessionGradingCommand;
import com.sep.vox.application.port.input.command.examevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.examevaluation.RecordExamAttemptEvaluationCommand;
import com.sep.vox.application.port.input.command.examevaluation.ValidityResultInput;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.exam.CompleteExamSessionGradingUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamItemEvaluationTurn;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
public class RecordExamAttemptEvaluationUseCase implements IUseCase<RecordExamAttemptEvaluationCommand, Void> {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private final CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate phaseOneTransactionTemplate;
    private final ConfidenceReviewCalculator confidenceReviewCalculator;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;

    public RecordExamAttemptEvaluationUseCase(
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricVersionRepository rubricVersionRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase,
            CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase,
            PlatformTransactionManager transactionManager,
            JsonSerializationPort jsonSerializationPort,
            ConfidenceReviewCalculator confidenceReviewCalculator,
            ClassTestGradingAssignmentService classTestGradingAssignmentService) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examItemEvaluationTurnRepository = examItemEvaluationTurnRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
        this.completeExamSessionGradingUseCase = completeExamSessionGradingUseCase;
        this.jsonSerializationPort = jsonSerializationPort;
        this.phaseOneTransactionTemplate = new TransactionTemplate(transactionManager);
        this.phaseOneTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.confidenceReviewCalculator = confidenceReviewCalculator;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
    }

    @Override
    public Void execute(RecordExamAttemptEvaluationCommand input) {
        var persisted = persistEvaluation(input);

        if (allResponsesHaveEvaluations(persisted.sessionId())) {
            var result = upsertExamCandidateResultUseCase.execute(persisted.sessionId());
            // Bài trên lớp: mở luôn phân công cho giáo viên chủ bài, để họ chấm được
            // ngay khi học sinh vừa nộp thay vì chờ đóng bài. No-op với kỳ thi tập trung.
            classTestGradingAssignmentService.ensureAssignmentForResult(result);
            completeExamSessionGradingUseCase.execute(
                new CompleteExamSessionGradingCommand(persisted.sessionId())
            );
        }
        return null;
    }

    private PersistedEvaluation persistEvaluation(RecordExamAttemptEvaluationCommand input) {
        var persisted = phaseOneTransactionTemplate.execute(status -> {
            var responseId = UUID.fromString(input.answerId());
            var response = examItemResponseRepository.findById(responseId)
                .orElseThrow(() -> new NotFoundException("không thể tìm thấy câu trả lời cho evaluation"));

            // Điểm người đã chốt thắng AI. Không có optimistic lock trên
            // exam_item_evaluations, nên một lượt chấm lại của AI (re-run, replay
            // Kafka) sẽ ghi đè bản FINALIZED và xoá luôn criterion scores của giáo
            // viên — mất im lặng, không ai phát hiện. Bỏ qua ở đây là chốt chặn:
            // muốn đổi điểm đã chốt thì đi qua chấm tay hoặc phúc khảo.
            if (hasFinalizedHumanEvaluation(response.getId())) {
                return new PersistedEvaluation(response.getSessionId());
            }

            var criteriaMap = input.payload() == null || input.payload().criteria() == null
                ? Map.<String, CriterionScoreInput>of()
                : input.payload().criteria();

            // Resolve the rubric version before combining criteria because each
            // school may use WEIGHTED_AVERAGE or SUM and a different score scale.
            // ExamPaperItem.weight remains a separate, later paper-level weight.
            var evaluationContext = resolveEvaluationContext(response.getSessionId());
            var rubricCriteriaByCode = rubricCriterionRepository.findByRubricVersionId(
                evaluationContext.rubricVersionId()
            ).stream().collect(Collectors.toMap(
                item -> normalizeCode(item.getCode()),
                Function.identity(),
                (left, right) -> left
            ));
            BigDecimal itemScore = computeItemScore(
                criteriaMap,
                rubricCriteriaByCode,
                evaluationContext.totalScoreMethod()
            );
            itemScore = clampScore(
                itemScore,
                evaluationContext.scoringScaleMin(),
                evaluationContext.scoringScaleMax()
            );

            var validity = input.payload() == null ? null : input.payload().validity();
            var signals = ExamEvaluationSignalMapper.toDomain(input.payload() == null ? null : input.payload().signals());
            var confidenceCase = signals.confidenceCase();
            // overallConfidence: min() của mọi c-value trong confidenceCase (severity-aware,
            // đúng tinh thần research -- không phải trung bình cộng). aiConfidence (trung bình
            // cộng cũ, KHÔNG phải kết quả research) đã bị bỏ hẳn -- nó từng gộp cả giá trị
            // sentinel "gọi LLM thất bại" (0.0) vào trung bình chung với giá trị thật, hiện sai
            // thành "AI confidence 25%/33%" trên UI dù chỉ là lỗi hạ tầng, không phải chấm kém
            // thật. Nếu confidenceCase chưa có (turn cũ trước khi case (1)-(5) được nối, hoặc
            // graph lỗi 1 phần), asrConfidenceAvg là tín hiệu thật duy nhất còn lại, dùng làm
            // fallback hiển thị -- vẫn null nếu không có, KHÔNG fabricate số.
            var overallConfidence = minimumConfidence(confidenceCase);
            if (overallConfidence == null) {
                overallConfidence = clampUnit(
                    input.payload() == null || input.payload().signals() == null
                        ? null
                        : input.payload().signals().asrConfidenceAvg()
                );
            }
            boolean isShortAnswer = signals.wordCount() < 35;
            var confidenceDecision = confidenceReviewCalculator.compute(
                confidenceCase,
                signals.audioQuality(),
                evaluationContext.examKind(),
                signals.codeSwitchingRatio(),
                isShortAnswer
            );
            var hasCriticalValidityFlag = hasCriticalValidityFlag(validity);
            boolean requiresHumanReview = hasCriticalValidityFlag || confidenceDecision.requiresHumanReview();
            String reviewReasonCode = hasCriticalValidityFlag
                ? "VALIDITY_FLAGGED"
                : (
                    confidenceDecision.requiresHumanReview()
                        ? String.join(",", confidenceDecision.reviewReasons())
                        : null
                );
            var requiresRetake = requiresRetake(validity);
            boolean markedInvalid = hasCriticalValidityFlag
                || (validity != null && Boolean.FALSE.equals(validity.validForScoring()));
            boolean zeroedByValidity = markedInvalid;
            if (zeroedByValidity) {
                itemScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            if ("uncooperative_move_on".equals(response.getTerminationReason())) {
                itemScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                requiresHumanReview = true;
                reviewReasonCode = "CONDUCT_VIOLATION";
                markedInvalid = true;
            }
            boolean zeroCriterionScores = markedInvalid;
            boolean insufficientEvidence = "INSUFFICIENT_EVIDENCE".equals(signals.evidenceStatus());
            String uncertaintyType;
            if (insufficientEvidence && requiresHumanReview) {
                uncertaintyType = "MIXED";
            } else if (insufficientEvidence) {
                uncertaintyType = "INSUFFICIENT_EVIDENCE";
            } else if (requiresHumanReview) {
                uncertaintyType = "SYSTEM_UNCERTAINTY";
            } else {
                uncertaintyType = "NONE";
            }
            signals = signals.withAssessment(
                uncertaintyType,
                confidenceDecision.confidenceMode(),
                confidenceDecision.audioGateStatus(),
                confidenceDecision.audioGateReasons()
            );

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
                    var score = zeroCriterionScores
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : clampCriterionScore(
                            BigDecimal.valueOf(entry.getValue().score()),
                            criterion
                        );
                    return new ExamItemCriterionScore(
                        savedEvaluation.getId(),
                        criterion.getId(),
                        score,
                        score,
                        entry.getValue().note(),
                        blankToNull(entry.getValue().matchedBandCode())
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
                    .map(turn -> {
                        var turnOrder = turn.turnOrder();
                        var wordCount = turn.wordCount();
                        return new ExamItemEvaluationTurn(
                            UUID.randomUUID(),
                            savedEvaluation.getId(),
                            turnOrder == null ? 0 : turnOrder,
                            parseTurnType(turn.turnType()),
                            turn.promptText(),
                            turn.audioUrl(),
                            turn.transcript() == null ? "" : turn.transcript(),
                            wordCount == null ? 0 : wordCount,
                            turn.durationSeconds(),
                            turn.asrConfidence(),
                            toJson(turn.pronunciationOverall()),
                            toJson(turn.wordFeedback())
                        );
                    })
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

    /**
     * Response đã có bản chấm tay đã chốt hay chưa. Dùng
     * {@code findLatestByResponseId} (chỉ trả AUTO_GRADED/FINALIZED) nên bản HUMAN
     * đã bị một vòng phúc khảo sau đó SUPERSEDED sẽ không chặn nhầm.
     */
    private boolean hasFinalizedHumanEvaluation(UUID responseId) {
        return examItemEvaluationRepository.findLatestByResponseId(responseId)
            .filter(evaluation -> evaluation.getEngineType() == ExamEvaluationEngineType.HUMAN)
            .filter(evaluation -> evaluation.getStatus() == ExamItemEvaluationStatus.FINALIZED)
            .isPresent();
    }

    private BigDecimal computeItemScore(
            Map<String, CriterionScoreInput> criteriaMap,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            RubricTotalScoreMethod totalScoreMethod) {
        var weightedSum = BigDecimal.ZERO;
        var weightSum = BigDecimal.ZERO;
        var matchedSum = BigDecimal.ZERO;
        var matchedCount = 0;
        var unweightedSum = BigDecimal.ZERO;
        var unweightedCount = 0;

        for (var entry : criteriaMap.entrySet()) {
            var criterionScore = entry.getValue();
            if (criterionScore == null || criterionScore.score() == null) {
                continue;
            }
            var criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
            if (criterion == null) {
                continue;
            }
            var score = clampCriterionScore(BigDecimal.valueOf(criterionScore.score()), criterion);
            unweightedSum = unweightedSum.add(score);
            unweightedCount++;

            matchedSum = matchedSum.add(score);
            matchedCount++;
            if (criterion.getWeight() == null) {
                continue;
            }
            weightedSum = weightedSum.add(score.multiply(criterion.getWeight()));
            weightSum = weightSum.add(criterion.getWeight());
        }

        if (totalScoreMethod == RubricTotalScoreMethod.SUM && matchedCount > 0) {
            return matchedSum.setScale(2, RoundingMode.HALF_UP);
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

    private BigDecimal clampScore(BigDecimal score, BigDecimal scaleMin, BigDecimal scaleMax) {
        var safeScore = score == null ? BigDecimal.ZERO : score;
        if (scaleMin != null && safeScore.compareTo(scaleMin) < 0) {
            safeScore = scaleMin;
        }
        if (scaleMax != null && safeScore.compareTo(scaleMax) > 0) {
            safeScore = scaleMax;
        }
        return safeScore.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal clampCriterionScore(BigDecimal score, RubricCriterion criterion) {
        return clampScore(score, criterion.getMinScore(), criterion.getMaxScore());
    }

    private EvaluationContext resolveEvaluationContext(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy phiên thi cho evaluation"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy bài kiểm tra cho evaluation"));
        if (exam.getAssessmentPolicyId() == null) {
            throw new NotFoundException("bài kiểm tra chưa gắn assessment policy");
        }
        var policy = assessmentPolicyRepository.findById(exam.getAssessmentPolicyId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy assessment policy cho bài kiểm tra"));
        var rubricVersion = rubricVersionRepository.findById(policy.getRubricVersionId())
            .orElseThrow(() -> new NotFoundException("không thể tìm thấy rubric version cho bài kiểm tra"));
        return new EvaluationContext(
            policy.getRubricVersionId(),
            exam.getKind(),
            rubricVersion.getTotalScoreMethod(),
            rubricVersion.getScoringScaleMin(),
            rubricVersion.getScoringScaleMax()
        );
    }

    private record EvaluationContext(
        UUID rubricVersionId,
        ExamKind examKind,
        RubricTotalScoreMethod totalScoreMethod,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax
    ) {
    }

    private boolean allResponsesHaveEvaluations(UUID sessionId) {
        return examItemResponseRepository.findBySessionId(sessionId).stream()
            .allMatch(item -> examItemEvaluationRepository.findLatestByResponseId(item.getId()).isPresent());
    }

    private Instant parseEvaluatedAt(String value) {
        return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return "discourse".equals(normalized) ? "coherence" : normalized;
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

    private BigDecimal clampUnit(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        var decimal = BigDecimal.valueOf(Math.max(0D, Math.min(1D, value)));
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    // overallConfidence hiển thị = min-gated (weakest-link, ĐÚNG research Vietnam-focus:
    // compass_artifact_wf-52df89c9 -- "overallReliability dùng min-gated, KHÔNG average", vì
    // weighted-average pha loãng tín hiệu cực xấu). NHƯNG chỉ gồm các tín hiệu ĐỘ TIN CẬY CHẤM ĐIỂM
    // (cAsrLog logprob-branch, cRef, cAlign, và 3 tiêu chí case-5). CỐ TÌNH LOẠI:
    //   - qSnr/qSpeech: là tín hiệu audio gate, KHÔNG phải confidence scalar. Audio đã hiển thị
    //     riêng ở field audioQuality và calculator dùng chúng độc lập cho quyết định review.
    //   - cPfBranch: = min(cRef, cAlign), đã có cRef/cAlign trong tập nên trùng lặp.
    private BigDecimal minimumConfidence(ConfidenceCaseSignals signals) {
        if (signals == null) {
            return null;
        }
        BigDecimal minimum = null;
        BigDecimal[] values = {
            signals.cAsrLog(),
            signals.cRef(),
            signals.cAlign(),
            signals.cGrammar(),
            signals.cVocabulary(),
            signals.cCoherence()
        };
        for (BigDecimal value : values) {
            if (value != null && (minimum == null || value.compareTo(minimum) < 0)) {
                minimum = value;
            }
        }
        return minimum == null ? null : minimum.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasCriticalValidityFlag(ValidityResultInput validity) {
        return validity != null
            && validity.overallSeverity() != null
            && "critical".equalsIgnoreCase(validity.overallSeverity());
    }

    private boolean requiresRetake(ValidityResultInput validity) {
        return validity != null
            && validity.action() != null
            && "reject_or_zero".equalsIgnoreCase(validity.action());
    }

    private String toJson(Object value) {
        return jsonSerializationPort.toJson(value == null ? List.of() : value);
    }

    private String toNullableJson(Object value) {
        return value == null ? null : jsonSerializationPort.toJson(value);
    }

    /**
     * Chuỗi RỖNG phải thành NULL trước khi xuống DB.
     *
     * findEstimatedResultBandOrder đã có sẵn chốt {@code matched_band_code IS NOT NULL} -- người
     * viết truy vấn đã lường trước chuyện thiếu mã bậc. Nhưng chuỗi rỗng LỌT QUA chốt đó rồi mới
     * chết ở phép nối {@code band.code = matched_band_code}: bản ghi biến mất khỏi phép ước
     * lượng, không lỗi, không log, chỉ là mẫu nhỏ đi. Đo trên dữ liệu thật: 5 dòng có mã, chỉ 3
     * dòng qua được phép nối, mà ngưỡng {@code total >= 5} lại đếm SAU khi nối -- nên phép ước
     * lượng bậc chưa từng chạy một lần nào.
     *
     * NULL hoá ở đây để chốt đó làm đúng việc nó được viết ra để làm.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
