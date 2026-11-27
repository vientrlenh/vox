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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.assessmentevaluation.AssessmentEvaluationSignalMapper;
import com.sep.vox.application.port.input.command.assessmentsession.CompleteAssessmentSessionGradingCommand;
import com.sep.vox.application.port.input.command.assessmentevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.assessmentevaluation.RecordAssessmentAttemptEvaluationCommand;
import com.sep.vox.application.port.input.command.assessmentevaluation.TurnDetailInput;
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
            // Class test: open the assignment for the teacher who owns the test right away, so they can grade
            // as soon as the student submits instead of waiting for the test to close. No-op for a centralized assessment.
            classTestGradingAssignmentService.ensureAssignmentForResult(result);
            completeAssessmentSessionGradingUseCase.execute(
                new CompleteAssessmentSessionGradingCommand(persisted.sessionId())
            );
        }
        return null;
    }

    private PersistedEvaluation persistEvaluation(RecordAssessmentAttemptEvaluationCommand input) {
        PersistedEvaluation persisted = requiresNewTransactionPort.callInNewTransaction(
            () -> persistEvaluationInNewTransaction(input));

        if (persisted == null) {
            throw new IllegalArgumentException("Cannot save the grading result for the answer");
        }
        return persisted;
    }

    private PersistedEvaluation persistEvaluationInNewTransaction(RecordAssessmentAttemptEvaluationCommand input) {
        UUID responseId = UUID.fromString(input.answerId());
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(responseId)
            .orElseThrow(() -> new ResourceNotFoundException("Could not find the answer for the evaluation"));

        // A score finalized by a person beats the AI. There is no optimistic lock on
        // assessment_item_evaluations, so a regrading run by the AI (re-run, Kafka replay)
        // would overwrite the FINALIZED version and also wipe the teacher's criterion scores --
        // a silent loss that nobody would notice. Skipping here is the safeguard:
        // to change a finalized score, go through manual grading or an appeal.
        if (hasFinalizedHumanEvaluation(response.getId())) {
            return new PersistedEvaluation(response.getSessionId());
        }

        Map<String, CriterionScoreInput> criteriaMap = criteriaOf(input);
        EvaluationContext evaluationContext = resolveEvaluationContext(response.getSessionId());
        Map<String, RubricCriterion> rubricCriteriaByCode = rubricCriteriaByCode(evaluationContext);
        GradedItem graded = gradeItem(input, response, evaluationContext, criteriaMap, rubricCriteriaByCode);

        AssessmentItemEvaluation savedEvaluation = saveEvaluation(input, response, graded);
        saveCriterionScores(savedEvaluation, criteriaMap, rubricCriteriaByCode, graded.markedInvalid());
        saveTurns(savedEvaluation, input);
        return new PersistedEvaluation(response.getSessionId());
    }

    private Map<String, CriterionScoreInput> criteriaOf(RecordAssessmentAttemptEvaluationCommand input) {
        return input.payload() == null || input.payload().criteria() == null
            ? Map.<String, CriterionScoreInput>of()
            : input.payload().criteria();
    }

    /**
     * The rubric version is resolved before the criteria are combined because each school may use
     * WEIGHTED_AVERAGE or SUM and a different score scale. AssessmentPaperItem.weight remains a
     * separate, later paper-level weight.
     */
    private Map<String, RubricCriterion> rubricCriteriaByCode(EvaluationContext evaluationContext) {
        return rubricCriterionRepository.findByRubricVersionId(
            evaluationContext.rubricVersionId()
        ).stream().collect(Collectors.toMap(
            item -> normalizeCode(item.getCode()),
            Function.identity(),
            (left, right) -> left
        ));
    }

    private GradedItem gradeItem(RecordAssessmentAttemptEvaluationCommand input, AssessmentItemResponse response,
            EvaluationContext evaluationContext, Map<String, CriterionScoreInput> criteriaMap,
            Map<String, RubricCriterion> rubricCriteriaByCode) {
        BigDecimal itemScore = computeItemScoreInContext(evaluationContext, criteriaMap, rubricCriteriaByCode);
        ValidityResultInput validity = input.payload() == null ? null : input.payload().validity();
        EvaluationSignals signals = AssessmentEvaluationSignalMapper.toDomain(input.payload() == null ? null : input.payload().signals());
        BigDecimal overallConfidence = overallConfidenceOf(input, signals.confidenceCase());
        boolean isShortAnswer = signals.wordCount() < 35;
        ConfidenceReviewCalculator.Decision confidenceDecision = confidenceReviewCalculator.compute(
            signals.confidenceCase(),
            signals.audioQuality(),
            evaluationContext.assessmentKind(),
            signals.codeSwitchingRatio(),
            isShortAnswer
        );
        ReviewVerdict verdict = reviewVerdict(
            response.getSessionId(), evaluationContext, validity, overallConfidence, confidenceDecision);
        return finalizeGradedItem(response, evaluationContext, validity, signals, itemScore, verdict);
    }

    /**
     * The clamp into the scale is ALREADY inside RubricItemScoreFormula (shared with the
     * manual-grading path), so there is no separate clampScore call here any more.
     */
    private BigDecimal computeItemScoreInContext(
            EvaluationContext evaluationContext,
            Map<String, CriterionScoreInput> criteriaMap,
            Map<String, RubricCriterion> rubricCriteriaByCode) {
        return computeItemScore(
            criteriaMap,
            rubricCriteriaByCode,
            evaluationContext.totalScoreMethod(),
            evaluationContext.scoringScaleMin(),
            evaluationContext.scoringScaleMax()
        );
    }

    private BigDecimal overallConfidenceOf(
            RecordAssessmentAttemptEvaluationCommand input, ConfidenceCaseSignals confidenceCase) {
        BigDecimal overallConfidence = averageConfidence(confidenceCase);
        if (overallConfidence == null) {
            overallConfidence = clampUnit(
                input.payload() == null || input.payload().signals() == null
                    ? null
                    : input.payload().signals().asrConfidenceAvg()
            );
        }
        return overallConfidence;
    }

    private ReviewVerdict reviewVerdict(
            UUID sessionId,
            EvaluationContext evaluationContext,
            ValidityResultInput validity,
            BigDecimal overallConfidence,
            ConfidenceReviewCalculator.Decision confidenceDecision) {
        boolean hasCriticalValidityFlag = hasCriticalValidityFlag(validity);
        BigDecimal thresholdPercent = evaluationContext.aiConfidenceThresholdPercent();
        boolean confidenceRequiresReview =
            confidenceRequiresReview(thresholdPercent, overallConfidence, confidenceDecision);
        boolean hasIntegrityProctoringAlert = hasIntegrityProctoringAlert(sessionId);
        boolean requiresHumanReview =
            hasCriticalValidityFlag || hasIntegrityProctoringAlert || confidenceRequiresReview;
        String reviewReasonCode = reviewReasonCode(
            hasCriticalValidityFlag, hasIntegrityProctoringAlert, confidenceRequiresReview,
            thresholdPercent, confidenceDecision);
        return new ReviewVerdict(overallConfidence, confidenceDecision, requiresHumanReview, reviewReasonCode);
    }

    /**
     * The school SETS a threshold -> SKIP all the hard rules of ConfidenceReviewCalculator and
     * compare exactly one number. The school has stated its acceptable level; stacking dozens of
     * internal thresholds on top would make the number they entered meaningless -- the attempt
     * would still be pushed to review for a reason they cannot see and cannot adjust.
     *
     * <p>The validity flag is NOT skipped: that is "can this answer be graded at all" (broken
     * audio, off-topic), not "how far can the grading be trusted". Two different questions.
     */
    private boolean confidenceRequiresReview(
            BigDecimal thresholdPercent,
            BigDecimal overallConfidence,
            ConfidenceReviewCalculator.Decision confidenceDecision) {
        // UNIT CONVERSION: overallConfidence is a 0-1 ratio while the threshold the school enters is
        // a percentage 0-100. Compared directly, 0.82 is always below 75 and EVERY attempt goes to
        // review -- a silent failure, since the system keeps running and humans just have to grade
        // everything by hand.
        BigDecimal confidencePercent = overallConfidence == null
            ? null
            : overallConfidence.multiply(ONE_HUNDRED);
        boolean belowThreshold = thresholdPercent != null
            // No confidence but a threshold -> send to review. Missing evidence is not evidence of
            // passing; lean toward a human looking again.
            && (confidencePercent == null || confidencePercent.compareTo(thresholdPercent) < 0);
        return thresholdPercent != null
            ? belowThreshold
            : confidenceDecision.requiresHumanReview();
    }

    /**
     * A proctoring alert that raises an INTEGRITY suspicion (several people in the frame, a phone detected,
     * a forbidden object) -> always sent to a reviewer. The list is in
     * AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES.
     *
     * <p>NOT skipped by the school threshold, for the same reason as the validity flag: this is the THIRD question,
     * completely different from the other two. The confidence threshold answers "how far can the grading be trusted", validity answers
     * "can this answer be graded at all", while this one answers "is this attempt trustworthy in terms of
     * integrity". An attempt graded by the AI very confidently (confidence 0.95) while the camera recorded two
     * people sitting together -- that 0.95 says nothing about who is speaking.
     *
     * <p>By TYPE, NOT by level. This gate used to ask level = 'CRITICAL', and when
     * MULTIPLE_PERSONS was lowered to WARNING (vox-streaming 4cc6598) exactly the example in the paragraph
     * above -- two people sitting together -- silently fell out of this rule. Nobody decided
     * that. Level is the scale of "does the proctor have to run over right away", a question about actions
     * during the session; it is not a scale for asking whether a finished attempt is trustworthy.
     *
     * <p>The list is DELIBERATELY narrow, keeping the spirit of the old version: losing the face from the camera or leaving the window
     * happens constantly in a real session (16 PERSON_MISSING events in one
     * session is a real measurement), and counting them too would send almost every attempt to waiting for review and
     * reviewing would lose its meaning.
     *
     * <p>The query is by SESSION so every answer in the same session gets the same result -- which is the intent:
     * the violation belongs to the whole session, not to a single answer.
     */
    private boolean hasIntegrityProctoringAlert(UUID sessionId) {
        return assessmentProctoringAlertRepository.hasIntegrityAlert(sessionId);
    }

    /**
     * Priority order of the reason code: validity -> proctoring -> confidence. Only ONE code is
     * shown, so the specific and actionable ones come first; "low confidence" is the vaguest of the
     * three and the one a reviewer can do least with.
     *
     * <p>PROCTORING_INTEGRITY, not PROCTORING_CRITICAL as before: the old code was named after the
     * alert's LEVEL, but level no longer decides this rule -- a reason code that contradicts the
     * condition producing it. Same reason vox-streaming renamed CRITICAL_VIOLATION to
     * UNCOOPERATIVE_CANDIDATE. Old records keep the old code (a ledger is not rewritten), so the
     * frontend label table must understand BOTH.
     */
    private String reviewReasonCode(
            boolean hasCriticalValidityFlag,
            boolean hasIntegrityProctoringAlert,
            boolean confidenceRequiresReview,
            BigDecimal thresholdPercent,
            ConfidenceReviewCalculator.Decision confidenceDecision) {
        return hasCriticalValidityFlag
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
    }

    /**
     * An invalidated attempt gets the SCALE FLOOR, not 0 (fixed 2026-08-11).
     *
     * <p>0 is only coincidentally right for a scale that starts at 0. A school that declares a 4-10
     * scale has 0 BELOW the floor, which drags section and session scores off the scale, and when
     * the total is off the scale no result band matches -- the student has a score but a blank
     * grade band.
     *
     * <p>The business meaning is unchanged: the scale floor IS the lowest score that rubric can
     * express. A school that wants "blank = 0" declares a floor of 0; that is their choice of scale,
     * not a decision of the system.
     */
    private GradedItem finalizeGradedItem(AssessmentItemResponse response, EvaluationContext evaluationContext,
            ValidityResultInput validity, EvaluationSignals signals, BigDecimal computedItemScore,
            ReviewVerdict verdict) {
        boolean markedInvalid = hasCriticalValidityFlag(validity)
            || (validity != null && Boolean.FALSE.equals(validity.validForScoring()));
        BigDecimal floorScore = scaleFloor(evaluationContext.scoringScaleMin());
        BigDecimal itemScore = markedInvalid ? floorScore : computedItemScore;
        boolean requiresHumanReview = verdict.requiresHumanReview();
        String reviewReasonCode = verdict.reviewReasonCode();
        if ("uncooperative_move_on".equals(response.getTerminationReason())) {
            itemScore = floorScore;
            requiresHumanReview = true;
            reviewReasonCode = "CONDUCT_VIOLATION";
            markedInvalid = true;
        }
        return new GradedItem(itemScore, verdict.overallConfidence(), requiresHumanReview, reviewReasonCode,
            markedInvalid, requiresRetake(validity),
            withAssessmentOutcome(signals, verdict.confidenceDecision(), requiresHumanReview), validity);
    }

    private EvaluationSignals withAssessmentOutcome(
            EvaluationSignals signals,
            ConfidenceReviewCalculator.Decision confidenceDecision,
            boolean requiresHumanReview) {
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
        return signals.withAssessment(
            uncertaintyType,
            confidenceDecision.confidenceMode(),
            confidenceDecision.audioGateStatus(),
            confidenceDecision.audioGateReasons()
        );
    }

    private AssessmentItemEvaluation saveEvaluation(
            RecordAssessmentAttemptEvaluationCommand input, AssessmentItemResponse response, GradedItem graded) {
        AssessmentItemEvaluation existingEvaluation = assessmentItemEvaluationRepository.findLatestByResponseId(response.getId()).orElse(null);
        AssessmentItemEvaluation evaluation = existingEvaluation == null
            ? newAutoGradedEvaluation(input, response, graded)
            : existingEvaluation;
        if (existingEvaluation != null) {
            overwriteWithAutoGrade(evaluation, input, response, graded);
        }

        AssessmentItemEvaluation savedEvaluation = assessmentItemEvaluationRepository.save(evaluation);
        if (existingEvaluation != null) {
            assessmentItemCriterionScoreRepository.deleteByEvaluationIdIn(List.of(savedEvaluation.getId()));
            assessmentItemEvaluationTurnRepository.deleteByEvaluationIdIn(List.of(savedEvaluation.getId()));
        }
        return savedEvaluation;
    }

    private AssessmentItemEvaluation newAutoGradedEvaluation(
            RecordAssessmentAttemptEvaluationCommand input, AssessmentItemResponse response, GradedItem graded) {
        return AssessmentItemEvaluation.autoGraded(
            response.getId(),
            response.getPaperItemId(),
            modelVersionOf(input),
            graded.itemScore(),
            graded.overallConfidence(),
            graded.requiresHumanReview(),
            graded.reviewReasonCode(),
            graded.markedInvalid(),
            graded.requiresRetake(),
            graded.signals(),
            toNullableJson(graded.validity()),
            feedbackSummaryOf(input),
            suggestionsJsonOf(input),
            promptVersionOf(input),
            evaluatedAtOf(input));
    }

    private void overwriteWithAutoGrade(
            AssessmentItemEvaluation evaluation,
            RecordAssessmentAttemptEvaluationCommand input,
            AssessmentItemResponse response,
            GradedItem graded) {
        evaluation.setResponseId(response.getId());
        evaluation.setPaperItemId(response.getPaperItemId());
        evaluation.setEngineType(AssessmentEvaluationEngineType.AI_SINGLE);
        evaluation.setGradedByModel(modelVersionOf(input));
        evaluation.setSampleCount(1);
        evaluation.setReviewerId(null);
        evaluation.setRawItemScore(graded.itemScore());
        evaluation.setItemScore(graded.itemScore());
        evaluation.setOverallConfidence(graded.overallConfidence());
        overwriteReviewOutcome(evaluation, input, graded);
    }

    private void overwriteReviewOutcome(
            AssessmentItemEvaluation evaluation, RecordAssessmentAttemptEvaluationCommand input, GradedItem graded) {
        evaluation.setRequiresHumanReview(graded.requiresHumanReview());
        evaluation.setReviewReasonCode(graded.reviewReasonCode());
        evaluation.setMarkedInvalid(graded.markedInvalid());
        evaluation.setRequiresRetake(graded.requiresRetake());
        evaluation.setSignals(graded.signals());
        evaluation.setValidityJson(toNullableJson(graded.validity()));
        evaluation.setFeedbackSummary(feedbackSummaryOf(input));
        evaluation.setSuggestionsJson(suggestionsJsonOf(input));
        evaluation.setPromptVersion(promptVersionOf(input));
        evaluation.setStatus(AssessmentItemEvaluationStatus.AUTO_GRADED);
        evaluation.setEvaluatedAt(evaluatedAtOf(input));
    }

    private String modelVersionOf(RecordAssessmentAttemptEvaluationCommand input) {
        return input.payload() == null ? "gpt-4o" : input.payload().modelVersion();
    }

    private String feedbackSummaryOf(RecordAssessmentAttemptEvaluationCommand input) {
        return input.payload() == null ? "" : input.payload().feedbackSummary();
    }

    private String suggestionsJsonOf(RecordAssessmentAttemptEvaluationCommand input) {
        return toJson(input.payload() == null ? List.of() : input.payload().suggestions());
    }

    private String promptVersionOf(RecordAssessmentAttemptEvaluationCommand input) {
        return input.payload() == null ? null : input.payload().promptVersion();
    }

    private Instant evaluatedAtOf(RecordAssessmentAttemptEvaluationCommand input) {
        return parseEvaluatedAt(input.payload() == null ? null : input.payload().evaluatedAt());
    }

    private void saveCriterionScores(
            AssessmentItemEvaluation savedEvaluation,
            Map<String, CriterionScoreInput> criteriaMap,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            boolean zeroCriterionScores) {
        List<AssessmentItemCriterionScore> criterionScores = criteriaMap.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().score() != null)
            .flatMap(entry -> toCriterionScore(savedEvaluation, entry, rubricCriteriaByCode, zeroCriterionScores).stream())
            .toList();
        if (!criterionScores.isEmpty()) {
            assessmentItemCriterionScoreRepository.saveAll(criterionScores);
        }
    }

    private Optional<AssessmentItemCriterionScore> toCriterionScore(
            AssessmentItemEvaluation savedEvaluation,
            Map.Entry<String, CriterionScoreInput> entry,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            boolean zeroCriterionScores) {
        RubricCriterion criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
        if (criterion == null) {
            return Optional.empty();
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
        return Optional.of(AssessmentItemCriterionScore.scored(
            savedEvaluation.getId(),
            criterion.getId(),
            score,
            entry.getValue().note(),
            blankToNull(entry.getValue().matchedBandCode())));
    }

    private void saveTurns(AssessmentItemEvaluation savedEvaluation, RecordAssessmentAttemptEvaluationCommand input) {
        List<AssessmentItemEvaluationTurn> turns = input.payload() == null || input.payload().turns() == null
            ? List.<AssessmentItemEvaluationTurn>of()
            : input.payload().turns().stream()
                .map(turn -> toEvaluationTurn(savedEvaluation, turn))
                .toList();
        if (!turns.isEmpty()) {
            assessmentItemEvaluationTurnRepository.saveAll(turns);
        }
    }

    private AssessmentItemEvaluationTurn toEvaluationTurn(AssessmentItemEvaluation savedEvaluation, TurnDetailInput turn) {
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
        return AssessmentItemEvaluationTurn.recorded(
            savedEvaluation.getId(),
            turnOrder == null ? 0 : turnOrder,
            parseTurnType(turn.turnType()),
            turn.promptText(),
            turn.audioUrl() == null ? "" : turn.audioUrl(),
            turn.transcript() == null ? "" : turn.transcript(),
            wordCount == null ? 0 : wordCount,
            turn.durationSeconds(),
            turn.asrConfidence(),
            toJson(turn.pronunciationOverall()),
            toJson(turn.wordFeedback()));
    }

    private record PersistedEvaluation(UUID sessionId) {
    }

    private record ReviewVerdict(
        BigDecimal overallConfidence,
        ConfidenceReviewCalculator.Decision confidenceDecision,
        boolean requiresHumanReview,
        String reviewReasonCode
    ) {
    }

    private record GradedItem(
        BigDecimal itemScore,
        BigDecimal overallConfidence,
        boolean requiresHumanReview,
        String reviewReasonCode,
        boolean markedInvalid,
        boolean requiresRetake,
        EvaluationSignals signals,
        ValidityResultInput validity
    ) {
    }

    /**
     * Whether the response already has a finalized manual grading. Uses
     * {@code findLatestByResponseId} (which only returns AUTO_GRADED/FINALIZED), so a HUMAN version
     * that a later appeal round has SUPERSEDED will not block by mistake.
     */
    private boolean hasFinalizedHumanEvaluation(UUID responseId) {
        return assessmentItemEvaluationRepository.findLatestByResponseId(responseId)
            .filter(evaluation -> evaluation.getEngineType() == AssessmentEvaluationEngineType.HUMAN)
            .filter(evaluation -> evaluation.getStatus() == AssessmentItemEvaluationStatus.FINALIZED)
            .isPresent();
    }

    /**
     * Resolves the criteria from the AI payload, then hands the computation to the formula SHARED with the
     * manual grading path ({@link RubricItemScoreFormula}) -- see the javadoc of that class for why they are merged.
     *
     * <p>What is specific to this path: a criterion in the payload that matches no code of the rubric is silently
     * SKIPPED, not thrown. Manual grading is the opposite (it throws) because a person is typing; here an unknown
     * code sent by Python must not be allowed to ruin the whole grading run.
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
     * The floor of the rubric scale -- the score for a submission that was invalidated (invalid, uncooperative).
     *
     * <p>If the scale is not declared, fall back to 0, keeping the old behavior for data with missing configuration.
     */
    private BigDecimal scaleFloor(BigDecimal scoringScaleMin) {
        return (scoringScaleMin == null ? BigDecimal.ZERO : scoringScaleMin)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private EvaluationContext resolveEvaluationContext(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Could not find the assessment session for the evaluation"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Could not find the assessment for the evaluation"));
        if (assessment.getAssessmentPolicyId() == null) {
            throw new ResourceNotFoundException("The assessment has no assessment policy attached");
        }
        AssessmentPolicy policy = assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
            .orElseThrow(() -> new ResourceNotFoundException("Could not find the assessment policy for the assessment"));
        RubricVersion rubricVersion = rubricVersionRepository.findById(policy.getRubricVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Could not find the rubric version for the assessment"));
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
        /** The confidence threshold the school set for this assessment. NULL = not set, use the hard rule as before. */
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

    /**
     * The displayed overallConfidence = min-gated (weakest-link, matching the Vietnam-focus research:
     * compass_artifact_wf-52df89c9 -- "overallReliability uses min-gated, NOT average", because a
     * weighted average dilutes an extremely bad signal). BUT it includes only the GRADING-RELIABILITY
     * signals (cAsrLog logprob-branch, cRef, cAlign, and the 3 case-5 criteria). DELIBERATELY EXCLUDED:
     * <ul>
     *   <li>qSnr/qSpeech: audio-gate signals, NOT a confidence scalar. Audio is already shown
     *       separately in the audioQuality field and the calculator uses them independently for the
     *       review decision.
     *   <li>cPfBranch: = min(cRef, cAlign), which are already in the set, so it would be a duplicate.
     * </ul>
     *
     * <p>SINCE 2026-08-11 cAlign is out of the set: case (4) is switched off (see
     * ConfidenceReviewCalculator), Python no longer emits c_align so the value is always null and
     * the loop skips it. The line is commented out instead of left idle -- keeping it would make OLD
     * records (which still have c_align) take the min over a signal the system no longer trusts,
     * i.e. two identical attempts get two different numbers just because they were graded at two
     * different times.
     */
    private BigDecimal averageConfidence(ConfidenceCaseSignals signals) {
        if (signals == null) {
            return null;
        }
        List<BigDecimal> presentValues = presentConfidenceValues(signals);
        BigDecimal total = presentValues.stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        return presentValues.isEmpty()
            ? null
            : total.divide(BigDecimal.valueOf(presentValues.size()), 2, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> presentConfidenceValues(ConfidenceCaseSignals signals) {
        BigDecimal[] values = {
            signals.cAsrLog(),
            signals.cRef(),
            // signals.cAlign(),
            signals.cGrammar(),
            signals.cVocabulary(),
            signals.cCoherence()
        };
        List<BigDecimal> presentValues = new ArrayList<BigDecimal>();
        for (BigDecimal value : values) {
            // SKIP null instead of treating it as 0: null means that signal was not emitted (case
            // switched off, or an old record), not "grading cannot be trusted". Adding 0 to the
            // numerator while still dividing by all 5 criteria would drag every attempt down just
            // because the system stopped using one signal -- and with a school-set threshold that
            // means sending attempts to review for no reason.
            if (value != null) {
                presentValues.add(value);
            }
        }
        return presentValues;
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
     * An EMPTY string must become NULL before it goes to the DB.
     *
     * findEstimatedResultBandOrder already has the guard {@code matched_band_code IS NOT NULL} -- whoever wrote the
     * query foresaw a missing band code. But an empty string SLIPS PAST that guard and then dies at the join
     * {@code band.code = matched_band_code}: the record vanishes from the estimate with no error and no log, the sample
     * just gets smaller. Measured on real data: 5 rows have a code, only 3 get through the join, and the threshold
     * {@code total >= 5} is counted AFTER the join -- so the band estimate has never run once.
     *
     * Nulling it here lets that guard do the job it was written for.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
