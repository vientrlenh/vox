package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * A teacher regrades -- submitting is final, there is no admin approval step.
 *
 * <p>Grades <em>each criterion of each part</em>, with no total score entered: each part
 * receives one HUMAN/FINALIZED version, every old version of those parts moves to SUPERSEDED, and then
 * {@link UpsertAssessmentCandidateResultUseCase} recomputes the total and the grade from all the items.
 * The total is therefore always a function of the items, in all four rounds.
 *
 * <p>The target status comes from {@code GradingRoundPolicy}, NOT left to
 * {@code resolveDefaultStatus}: in the post-check round the attempt must stay RELEASED,
 * while the default would pull it back to PENDING_REVIEW if any item still needs a look -- i.e. taking back
 * the student's already published score for a technical reason.
 */
@Service
@RequiredArgsConstructor
public class RegradeResultUseCase implements IUseCase<SubmitGradingCommand, GradingActionResponse> {

    /**
     * assessment_item_evaluations.graded_by_model is NOT NULL and is meant for the name of an AI model;
     * a human grader has no model so the engine name stands in. The real grader is in
     * {@code reviewerId}.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final GradingActionSupport gradingActionSupport;
    private final GradingItemScoreResolver gradingItemScoreResolver;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;

    @Override
    @Transactional
    public GradingActionResponse execute(SubmitGradingCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.REGRADED, null);
        AssessmentGradingAccessService.GradingContext context = prepared.context();

        List<GradingItemScoreResolver.ResolvedItem> resolvedItems = resolveEveryPart(context, command);
        Instant now = Instant.now();
        clearSuspicionFlag(context.session());
        supersedePreviousEvaluations(resolvedItems);
        saveHumanEvaluations(resolvedItems, prepared.currentUserId(), now);

        AssessmentCandidateResult recalculated = recalculateResult(prepared);
        gradingActionSupport.finish(prepared, recalculated);

        return new GradingActionResponse(
            command.assignmentId(),
            recalculated.getId(),
            GradingOutcome.REGRADED.name(),
            recalculated.getStatus() == null ? null : recalculated.getStatus().name(),
            recalculated.getTotalScore(),
            null
        );
    }

    /**
     * Submitting finalizes as COMPLETED, so every part must be covered -- submitting partially and then
     * finalizing would lock hard the ungraded part (a COMPLETED attempt cannot be undone).
     */
    private List<GradingItemScoreResolver.ResolvedItem> resolveEveryPart(
            AssessmentGradingAccessService.GradingContext context, SubmitGradingCommand command) {
        return gradingItemScoreResolver.resolve(context, command, true);
    }

    /** Clears the suspicion flag: the teacher has looked at it and concluded there is no violation. */
    private void clearSuspicionFlag(AssessmentSession session) {
        if (session.isFlagged()) {
            session.setFlagged(false);
            assessmentSessionRepository.save(session);
        }
    }

    /**
     * The AI version and every older grading of the graded parts all move back to SUPERSEDED, so each part
     * has exactly one FINALIZED version as its score source. The calculator relies on old versions having
     * flipped status; it does NOT prefer HUMAN by engineType.
     */
    private void supersedePreviousEvaluations(List<GradingItemScoreResolver.ResolvedItem> resolvedItems) {
        List<UUID> responseIds = resolvedItems.stream()
            .map(item -> item.responseId()).toList();
        for (AssessmentItemEvaluation evaluation : assessmentItemEvaluationRepository.findByResponseIdIn(responseIds)) {
            if (evaluation.getStatus() != AssessmentItemEvaluationStatus.SUPERSEDED) {
                evaluation.setStatus(AssessmentItemEvaluationStatus.SUPERSEDED);
                assessmentItemEvaluationRepository.save(evaluation);
            }
        }
    }

    private void saveHumanEvaluations(
            List<GradingItemScoreResolver.ResolvedItem> resolvedItems, UUID currentUserId, Instant now) {
        List<AssessmentItemCriterionScore> criterionScores = new ArrayList<AssessmentItemCriterionScore>();
        for (GradingItemScoreResolver.ResolvedItem item : resolvedItems) {
            AssessmentItemEvaluation savedEvaluation = assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.humanGraded(
                    item.responseId(),
                    item.paperItemId(),
                    HUMAN_GRADER,
                    currentUserId,
                    item.itemScore(),
                    item.feedbackSummary(),
                    now));
            item.criterionScores().forEach(score -> criterionScores.add(AssessmentItemCriterionScore.scoredWithoutBand(
                    savedEvaluation.getId(),
                    score.rubricCriterionId(),
                    score.score(),
                    score.rationale())));
        }
        assessmentItemCriterionScoreRepository.saveAll(criterionScores);
    }

    /**
     * One call is enough: the calculator scans every item so it sees all the new versions. The status is
     * passed explicitly -- see the class javadoc.
     *
     * <p>It NO LONGER publishes HumanGradingSubmittedEvent to infer weakness labels from the teacher's
     * feedbackSummary. Both the listener (GradingDiagnosticsInferenceJob) and the destination
     * (weakness_observation) have been removed; keeping it would still make every teacher submission build an
     * item list and call an LLM for a bottomless box.
     */
    private AssessmentCandidateResult recalculateResult(GradingActionSupport.PreparedAction prepared) {
        AssessmentCandidateResultStatus targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), GradingOutcome.REGRADED);
        return upsertAssessmentCandidateResultUseCase.execute(
            prepared.context().candidateResult().getSessionId(),
            targetStatus == null ? prepared.context().candidateResult().getStatus() : targetStatus);
    }

    /** Which rounds allow re-grading -- used in documentation and tests, kept next to the rule for easy lookup. */
    static boolean supports(GradingRoundType roundType) {
        return GradingRoundPolicy.isAllowed(roundType, GradingOutcome.REGRADED);
    }
}
