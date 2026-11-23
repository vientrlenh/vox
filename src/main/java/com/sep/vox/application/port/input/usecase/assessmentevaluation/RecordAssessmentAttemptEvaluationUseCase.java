package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.valueobject.EvaluationSignals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.assessmentevaluation.AssessmentEvaluationSignalMapper;
import com.sep.vox.application.port.input.command.CompleteAssessmentSessionGradingCommand;
import com.sep.vox.application.port.input.command.assessmentevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.RecordAssessmentAttemptEvaluationCommand;
import com.sep.vox.application.port.input.command.assessmentevaluation.ValidityResultInput;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CompleteAssessmentSessionGradingUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.service.assessment.RubricItemScoreFormula;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
@RequiredArgsConstructor
public class RecordAssessmentAttemptEvaluationUseCase implements IUseCase<RecordAssessmentAttemptEvaluationCommand, Void> {

    /** Factor that converts the 0-1 ratio (overall_confidence) to a percentage (the threshold the school enters). */
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private final AssessmentItemEvaluationTurnRepository assessmentItemEvaluationTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
    private final CompleteAssessmentSessionGradingUseCase completeAssessmentSessionGradingUseCase;
    private final RequiresNewTransactionPort requiresNewTransactionPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final ConfidenceReviewCalculator confidenceReviewCalculator;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;

    @Override
    public Void execute(RecordAssessmentAttemptEvaluationCommand input) {
        PersistedEvaluation persisted = persistEvaluation(input);

        if (allResponsesHaveEvaluations(persisted.sessionId())) {
            AssessmentCandidateResult result = upsertAssessmentCandidateResultUseCase.execute(persisted.sessionId());
            // Bài trên lớp: mở luôn phân công cho giáo viên chủ bài, để họ chấm được
            // ngay khi học sinh vừa nộp thay vì chờ đóng bài. No-op với kỳ thi tập trung.
            classTestGradingAssignmentService.ensureAssignmentForResult(result);
            completeAssessmentSessionGradingUseCase.execute(
                new CompleteAssessmentSessionGradingCommand(persisted.sessionId())
            );
        }
        return null;
    }

    private PersistedEvaluation persistEvaluation(RecordAssessmentAttemptEvaluationCommand input) {
        PersistedEvaluation persisted = requiresNewTransactionPort.callInNewTransaction(() -> {
            UUID responseId = UUID.fromString(input.answerId());
            AssessmentItemResponse response = assessmentItemResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy câu trả lời cho evaluation"));

            // A score finalized by a person beats the AI. There is no optimistic lock on
            // assessment_item_evaluations, so a regrading run by the AI (re-run, Kafka replay)
            // would overwrite the FINALIZED version and also wipe the teacher's criterion scores --
            // a silent loss that nobody would notice. Skipping here is the safeguard:
            // to change a finalized score, go through manual grading or an appeal.
            if (hasFinalizedHumanEvaluation(response.getId())) {
                return new PersistedEvaluation(response.getSessionId());
            }

            Map<String, CriterionScoreInput> criteriaMap = input.payload() == null || input.payload().criteria() == null
                ? Map.<String, CriterionScoreInput>of()
                : input.payload().criteria();

            // Resolve the rubric version before combining criteria because each
            // school may use WEIGHTED_AVERAGE or SUM and a different score scale.
            // AssessmentPaperItem.weight remains a separate, later paper-level weight.
            EvaluationContext evaluationContext = resolveEvaluationContext(response.getSessionId());
            Map<String, RubricCriterion> rubricCriteriaByCode = rubricCriterionRepository.findByRubricVersionId(
                evaluationContext.rubricVersionId()
            ).stream().collect(Collectors.toMap(
                item -> normalizeCode(item.getCode()),
                Function.identity(),
                (left, right) -> left
            ));
            // Kẹp về trong thang nay nằm SẴN trong RubricItemScoreFormula (dùng chung với đường
            // chấm tay), nên không còn lời gọi clampScore riêng ở đây nữa.
            BigDecimal itemScore = computeItemScore(
                criteriaMap,
                rubricCriteriaByCode,
                evaluationContext.totalScoreMethod(),
                evaluationContext.scoringScaleMin(),
                evaluationContext.scoringScaleMax()
            );

            ValidityResultInput validity = input.payload() == null ? null : input.payload().validity();
            EvaluationSignals signals = AssessmentEvaluationSignalMapper.toDomain(input.payload() == null ? null : input.payload().signals());
            ConfidenceCaseSignals confidenceCase = signals.confidenceCase();
            BigDecimal overallConfidence = averageConfidence(confidenceCase);
            if (overallConfidence == null) {
                overallConfidence = clampUnit(
                    input.payload() == null || input.payload().signals() == null
                        ? null
                        : input.payload().signals().asrConfidenceAvg()
                );
            }
            boolean isShortAnswer = signals.wordCount() < 35;
            ConfidenceReviewCalculator.Decision confidenceDecision = confidenceReviewCalculator.compute(
                confidenceCase,
                signals.audioQuality(),
                evaluationContext.assessmentKind(),
                signals.codeSwitchingRatio(),
                isShortAnswer
            );
            boolean hasCriticalValidityFlag = hasCriticalValidityFlag(validity);
            // Nhà trường ĐẶT ngưỡng -> BỎ QUA toàn bộ luật cứng của ConfidenceReviewCalculator,
            // chỉ so đúng một con số. Nhà trường đã nói rõ mức chấp nhận được của họ; chồng thêm
            // hàng chục ngưỡng nội bộ lên trên nữa thì con số họ nhập không còn nghĩa gì -- bài
            // vẫn bị đẩy sang duyệt vì một lý do họ không thấy và không chỉnh được.
            //
            // KHÔNG bỏ qua cờ validity: đó là "câu này có chấm được không" (audio hỏng, lạc đề),
            // không phải "chấm tin được tới đâu". Hai câu hỏi khác nhau.
            BigDecimal thresholdPercent = evaluationContext.aiConfidenceThresholdPercent();
            // QUY ĐỔI ĐƠN VỊ: overallConfidence là tỉ lệ 0-1, ngưỡng nhà trường nhập là phần trăm
            // 0-100. So thẳng thì 0.82 luôn nhỏ hơn 75 và MỌI bài đổ sang duyệt -- hỏng im lặng,
            // vì hệ thống vẫn chạy và chỉ có con người phải chấm tay tất cả.
            BigDecimal confidencePercent = overallConfidence == null
                ? null
                : overallConfidence.multiply(ONE_HUNDRED);
            boolean belowThreshold = thresholdPercent != null
                // Không có confidence mà lại có ngưỡng -> đưa sang duyệt. Thiếu bằng chứng không
                // phải là bằng chứng đạt; nghiêng về phía con người xem lại.
                && (confidencePercent == null || confidencePercent.compareTo(thresholdPercent) < 0);
            boolean confidenceRequiresReview = thresholdPercent != null
                ? belowThreshold
                : confidenceDecision.requiresHumanReview();
            // A proctoring alert that raises an INTEGRITY suspicion (several people in the frame, a phone detected,
            // a forbidden object) -> always sent to a reviewer. The list is in
            // AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES.
            //
            // NOT skipped by the school threshold, for the same reason as the validity flag: this is the THIRD question,
            // completely different from the other two. The confidence threshold answers "how far can the grading be trusted", validity answers
            // "can this answer be graded at all", while this one answers "is this attempt trustworthy in terms of
            // integrity". An attempt graded by the AI very confidently (confidence 0.95) while the camera recorded two
            // people sitting together -- that 0.95 says nothing about who is speaking.
            //
            // By TYPE, NOT by level. This gate used to ask level = 'CRITICAL', and when
            // MULTIPLE_PERSONS was lowered to WARNING (vox-streaming 4cc6598) exactly the example in the paragraph
            // above -- two people sitting together -- silently fell out of this rule. Nobody decided
            // that. Level is the scale of "does the proctor have to run over right away", a question about actions
            // during the session; it is not a scale for asking whether a finished attempt is trustworthy.
            //
            // The list is DELIBERATELY narrow, keeping the spirit of the old version: losing the face from the camera or leaving the window
            // happens constantly in a real session (16 PERSON_MISSING events in one
            // session is a real measurement), and counting them too would send almost every attempt to waiting for review and
            // reviewing would lose its meaning.
            //
            // The query is by SESSION so every answer in the same session gets the same result -- which is the intent:
            // the violation belongs to the whole session, not to a single answer.
            boolean hasIntegrityProctoringAlert =
                assessmentProctoringAlertRepository.hasIntegrityAlert(response.getSessionId());
            boolean requiresHumanReview =
                hasCriticalValidityFlag || hasIntegrityProctoringAlert || confidenceRequiresReview;
            // Thứ tự ưu tiên của mã lý do: validity -> giám sát -> tin cậy. Chỉ hiện MỘT mã, nên
            // xếp cái cụ thể và hành động được lên trước; "điểm tin cậy thấp" là lý do mơ hồ nhất
            // trong ba cái và cũng là cái người soát ít làm được gì nhất.
            // PROCTORING_INTEGRITY, không phải PROCTORING_CRITICAL như trước: mã cũ đặt tên theo MỨC
            // ĐỘ của cảnh báo, mà mức độ giờ không còn là thứ quyết định luật này -- một mã lý do tự
            // mâu thuẫn với chính điều kiện sinh ra nó. Cùng lý do vox-streaming đổi
            // CRITICAL_VIOLATION thành UNCOOPERATIVE_CANDIDATE. Bản ghi cũ giữ nguyên mã cũ (sổ thì
            // không viết lại), nên bảng nhãn phía FE phải hiểu CẢ HAI.
            String reviewReasonCode = hasCriticalValidityFlag
                ? "VALIDITY_FLAGGED"
                : hasIntegrityProctoringAlert
                    ? "PROCTORING_INTEGRITY"
                    : (
                        !confidenceRequiresReview
                            ? null
                            : thresholdPercent != null
                                ? "CONFIDENCE_BELOW_THRESHOLD"
                                : String.join(",", confidenceDecision.reviewReasons())
                    );
            boolean requiresRetake = requiresRetake(validity);
            boolean markedInvalid = hasCriticalValidityFlag
                || (validity != null && Boolean.FALSE.equals(validity.validForScoring()));
            // Bài vô hiệu nhận SÀN CỦA THANG, không phải số 0 (sửa 2026-08-11).
            //
            // Số 0 chỉ tình cờ đúng với thang bắt đầu từ 0. Trường khai thang 4-10 thì 0 nằm DƯỚI
            // sàn, kéo điểm phần và điểm phiên tụt ra ngoài thang, và khi tổng điểm ngoài thang thì
            // không dải xếp loại nào khớp -- học sinh có điểm mà bỏ trống xếp loại.
            //
            // Nghĩa nghiệp vụ không đổi: sàn thang LÀ điểm thấp nhất diễn đạt được của rubric đó.
            // Trường nào muốn "bỏ trống = 0" thì khai sàn thang bằng 0, đó là lựa chọn của họ về
            // thang điểm chứ không phải quyết định của hệ thống.
            BigDecimal floorScore = scaleFloor(evaluationContext.scoringScaleMin());
            boolean zeroedByValidity = markedInvalid;
            if (zeroedByValidity) {
                itemScore = floorScore;
            }
            if ("uncooperative_move_on".equals(response.getTerminationReason())) {
                itemScore = floorScore;
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

            AssessmentItemEvaluation existingEvaluation = assessmentItemEvaluationRepository.findLatestByResponseId(response.getId()).orElse(null);
            AssessmentItemEvaluation evaluation = existingEvaluation == null
                ? AssessmentItemEvaluation.builder()
                        .responseId(response.getId())
                        .paperItemId(response.getPaperItemId())
                        .engineType(AssessmentEvaluationEngineType.AI_SINGLE)
                        .gradedByModel(input.payload() == null ? "gpt-4o" : input.payload().modelVersion())
                        .sampleCount(1)
                        .reviewerId(null)
                        .rawItemScore(itemScore)
                        .itemScore(itemScore)
                        .overallConfidence(overallConfidence)
                        .requiresHumanReview(requiresHumanReview)
                        .reviewReasonCode(reviewReasonCode)
                        .markedInvalid(markedInvalid)
                        .requiresRetake(requiresRetake)
                        .signals(signals)
                        .validityJson(toNullableJson(validity))
                        .feedbackSummary(input.payload() == null ? "" : input.payload().feedbackSummary())
                        .suggestionsJson(toJson(input.payload() == null ? List.of() : input.payload().suggestions()))
                        .promptVersion(input.payload() == null ? null : input.payload().promptVersion())
                        .status(AssessmentItemEvaluationStatus.AUTO_GRADED)
                        .evaluatedAt(parseEvaluatedAt(input.payload() == null ? null : input.payload().evaluatedAt()))
                        .build()
                : existingEvaluation;

            if (existingEvaluation != null) {
                evaluation.setResponseId(response.getId());
                evaluation.setPaperItemId(response.getPaperItemId());
                evaluation.setEngineType(AssessmentEvaluationEngineType.AI_SINGLE);
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
                evaluation.setStatus(AssessmentItemEvaluationStatus.AUTO_GRADED);
                evaluation.setEvaluatedAt(parseEvaluatedAt(input.payload() == null ? null : input.payload().evaluatedAt()));
            }

            AssessmentItemEvaluation savedEvaluation = assessmentItemEvaluationRepository.save(evaluation);
            if (existingEvaluation != null) {
                assessmentItemCriterionScoreRepository.deleteByEvaluationIdIn(List.of(savedEvaluation.getId()));
                assessmentItemEvaluationTurnRepository.deleteByEvaluationIdIn(List.of(savedEvaluation.getId()));
            }

            List<AssessmentItemCriterionScore> criterionScores = criteriaMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().score() != null)
                .map(entry -> {
                    RubricCriterion criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
                    if (criterion == null) {
                        return null;
                    }
                    // The floor of THAT criterion, not the number 0 -- same reason as the question score above.
                    // Use the criterion's min (not the scale's) because with SUM every criterion has its
                    // own range, and the score shown for each criterion must lie within its range.
                    BigDecimal score = zeroCriterionScores
                        ? clampCriterionScore(criterion.getMinScore(), criterion)
                        : clampCriterionScore(
                            BigDecimal.valueOf(entry.getValue().score()),
                            criterion
                        );
                    return AssessmentItemCriterionScore.builder()
                        .evaluationId(savedEvaluation.getId())
                        .rubricCriterionId(criterion.getId())
                        .rawScore(score)
                        .finalScore(score)
                        .rationale(entry.getValue().note())
                        .matchedBandCode(blankToNull(entry.getValue().matchedBandCode()))
                        .build();
                })
                .filter(item -> item != null)
                .toList();
            if (!criterionScores.isEmpty()) {
                assessmentItemCriterionScoreRepository.saveAll(criterionScores);
            }

            List<AssessmentItemEvaluationTurn> turns = input.payload() == null || input.payload().turns() == null
                ? List.<AssessmentItemEvaluationTurn>of()
                : input.payload().turns().stream()
                    .map(turn -> {
                        Integer turnOrder = turn.turnOrder();
                        Integer wordCount = turn.wordCount();
                        // The same guard as the transcript right below, and for exactly the same reason: the column
                        // assessment_item_evaluation_turns.audio_url is NOT NULL (V1__baseline),
                        // so a turn without a recording would throw right at saveAll and bring down the WHOLE transaction that saves the grading result --
                        // losing the scores of the other turns that have no problem at all.
                        //
                        // A turn missing its audio is real, not hypothetical: a turn
                        // published before the recording made it back (see
                        // turn_publisher.publish_turn_if_new, phase 1) carries audio_url null.
                        return AssessmentItemEvaluationTurn.builder()
                            .id(UUID.randomUUID())
                            .evaluationId(savedEvaluation.getId())
                            .turnOrder(turnOrder == null ? 0 : turnOrder)
                            .turnType(parseTurnType(turn.turnType()))
                            .promptText(turn.promptText())
                            .audioUrl(turn.audioUrl() == null ? "" : turn.audioUrl())
                            .transcript(turn.transcript() == null ? "" : turn.transcript())
                            .wordCount(wordCount == null ? 0 : wordCount)
                            .durationSeconds(turn.durationSeconds())
                            .asrConfidence(turn.asrConfidence())
                            .pronunciationOverallJson(toJson(turn.pronunciationOverall()))
                            .wordFeedbackJson(toJson(turn.wordFeedback()))
                            .build();
                    })
                    .toList();
            if (!turns.isEmpty()) {
                assessmentItemEvaluationTurnRepository.saveAll(turns);
            }

            return new PersistedEvaluation(response.getSessionId());
        });

        if (persisted == null) {
            throw new IllegalArgumentException("Không thể lưu kết quả chấm cho câu trả lời");
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
        return assessmentItemEvaluationRepository.findLatestByResponseId(responseId)
            .filter(evaluation -> evaluation.getEngineType() == AssessmentEvaluationEngineType.HUMAN)
            .filter(evaluation -> evaluation.getStatus() == AssessmentItemEvaluationStatus.FINALIZED)
            .isPresent();
    }

    /**
     * Phân giải tiêu chí từ payload của AI rồi giao phép tính cho công thức DÙNG CHUNG với đường
     * chấm tay ({@link RubricItemScoreFormula}) -- xem lý do gộp ở javadoc của lớp đó.
     *
     * <p>Phần riêng của đường này: tiêu chí trong payload không khớp mã nào của rubric thì BỎ QUA
     * im lặng, không ném. Chấm tay thì ngược lại (ném lỗi) vì đó là người đang nhập; còn ở đây một
     * mã lạ do Python gửi lên không được phép làm hỏng cả lượt chấm.
     */
    private BigDecimal computeItemScore(
            Map<String, CriterionScoreInput> criteriaMap,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            RubricTotalScoreMethod totalScoreMethod,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax) {
        List<RubricItemScoreFormula.ScoredCriterion> scored = new ArrayList<RubricItemScoreFormula.ScoredCriterion>();
        for (Map.Entry<String, CriterionScoreInput> entry : criteriaMap.entrySet()) {
            CriterionScoreInput criterionScore = entry.getValue();
            if (criterionScore == null || criterionScore.score() == null) {
                continue;
            }
            RubricCriterion criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
            if (criterion == null) {
                continue;
            }
            scored.add(new RubricItemScoreFormula.ScoredCriterion(
                clampCriterionScore(BigDecimal.valueOf(criterionScore.score()), criterion),
                criterion.getWeight()
            ));
        }
        return RubricItemScoreFormula.compute(
            scored, scoringScaleMin, scoringScaleMax
        );
    }

    private BigDecimal clampScore(BigDecimal score, BigDecimal scaleMin, BigDecimal scaleMax) {
        BigDecimal safeScore = score == null ? BigDecimal.ZERO : score;
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

    /**
     * Sàn của thang rubric -- điểm dành cho bài bị vô hiệu hoá (không hợp lệ, không hợp tác).
     *
     * <p>Chưa khai thang thì lùi về 0, giữ nguyên hành vi cũ cho dữ liệu thiếu cấu hình.
     */
    private BigDecimal scaleFloor(BigDecimal scoringScaleMin) {
        return (scoringScaleMin == null ? BigDecimal.ZERO : scoringScaleMin)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private EvaluationContext resolveEvaluationContext(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy phiên thi cho evaluation"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy bài kiểm tra cho evaluation"));
        if (assessment.getAssessmentPolicyId() == null) {
            throw new ResourceNotFoundException("bài kiểm tra chưa gắn assessment policy");
        }
        AssessmentPolicy policy = assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy assessment policy cho bài kiểm tra"));
        RubricVersion rubricVersion = rubricVersionRepository.findById(policy.getRubricVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("không thể tìm thấy rubric version cho bài kiểm tra"));
        return new EvaluationContext(
            policy.getRubricVersionId(),
            assessment.getKind(),
            rubricVersion.getTotalScoreMethod(),
            rubricVersion.getScoringScaleMin(),
            rubricVersion.getScoringScaleMax(),
            assessment.getAiConfidenceThresholdPercent()
        );
    }

    private record EvaluationContext(
        UUID rubricVersionId,
        AssessmentKind assessmentKind,
        RubricTotalScoreMethod totalScoreMethod,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        /** Ngưỡng tin cậy nhà trường đặt cho bài này. NULL = không đặt, dùng luật cứng như cũ. */
        BigDecimal aiConfidenceThresholdPercent
    ) {
    }

    private boolean allResponsesHaveEvaluations(UUID sessionId) {
        return assessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .allMatch(item -> assessmentItemEvaluationRepository.findLatestByResponseId(item.getId()).isPresent());
    }

    private Instant parseEvaluatedAt(String value) {
        return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
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
        BigDecimal decimal = BigDecimal.valueOf(Math.max(0D, Math.min(1D, value)));
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    // overallConfidence hiển thị = min-gated (weakest-link, ĐÚNG research Vietnam-focus:
    // compass_artifact_wf-52df89c9 -- "overallReliability dùng min-gated, KHÔNG average", vì
    // weighted-average pha loãng tín hiệu cực xấu). NHƯNG chỉ gồm các tín hiệu ĐỘ TIN CẬY CHẤM ĐIỂM
    // (cAsrLog logprob-branch, cRef, cAlign, và 3 tiêu chí case-5). CỐ TÌNH LOẠI:
    //   - qSnr/qSpeech: là tín hiệu audio gate, KHÔNG phải confidence scalar. Audio đã hiển thị
    //     riêng ở field audioQuality và calculator dùng chúng độc lập cho quyết định review.
    //   - cPfBranch: = min(cRef, cAlign), đã có cRef/cAlign trong tập nên trùng lặp.
    //
    // TỪ 2026-08-11 cAlign bị loại khỏi tập: case (4) đã tắt (xem ConfidenceReviewCalculator),
    // Python không phát ra c_align nữa nên giá trị luôn null và vòng lặp bỏ qua. Chú thích hẳn
    // dòng đó thay vì để nó nằm im -- giữ lại thì bản ghi CŨ (còn c_align) sẽ được tính min theo
    // một tín hiệu mà hệ thống đã ngừng tin dùng, tức hai bài giống nhau cho hai con số khác nhau
    // chỉ vì chấm ở hai thời điểm.
    private BigDecimal averageConfidence(ConfidenceCaseSignals signals) {
        if (signals == null) {
            return null;
        }
        BigDecimal[] values = {
            signals.cAsrLog(),
            signals.cRef(),
            // signals.cAlign(),
            signals.cGrammar(),
            signals.cVocabulary(),
            signals.cCoherence()
        };
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal value : values) {
            // BỎ QUA null chứ không coi là 0: null nghĩa là tín hiệu đó không được phát ra (case
            // đã tắt, hoặc bản ghi cũ), không phải "chấm không tin được". Cộng 0 vào tử số mà vẫn
            // chia cho toàn bộ 5 tiêu chí sẽ kéo mọi bài xuống chỉ vì hệ thống ngừng dùng một
            // tín hiệu -- và với ngưỡng nhà trường đặt thì đó là đẩy bài sang duyệt vô cớ.
            if (value != null) {
                total = total.add(value);
                count++;
            }
        }
        return count == 0
            ? null
            : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
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
