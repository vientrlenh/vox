package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingActionSupport.PreparedAction;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver.ResolvedItem;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RegradeResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * The most complex use case of the feature: it both overrides the AI's ruling and decides which
 * status the submission goes to. The two easiest places to get wrong are locked here -- if the old version is not
 * SUPERSEDED the old score is still counted, and if the target status takes the wrong default the post-check
 * would withdraw a score already published to the student.
 */
class RegradeResultUseCaseTests {

    private GradingActionSupport gradingActionSupport;
    private GradingItemScoreResolver gradingItemScoreResolver;
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
    private RegradeResultUseCase useCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();

    private AssessmentSession session;
    private AssessmentCandidateResult recalculated;

    @BeforeEach
    void setUp() {
        gradingActionSupport = mock(GradingActionSupport.class);
        gradingItemScoreResolver = mock(GradingItemScoreResolver.class);
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        assessmentItemCriterionScoreRepository = mock(AssessmentItemCriterionScoreRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        upsertAssessmentCandidateResultUseCase = mock(UpsertAssessmentCandidateResultUseCase.class);
        useCase = new RegradeResultUseCase(
            gradingActionSupport,
            gradingItemScoreResolver,
            assessmentItemEvaluationRepository,
            assessmentItemCriterionScoreRepository,
            assessmentSessionRepository,
            upsertAssessmentCandidateResultUseCase);

        session = new AssessmentSession();
        session.setId(sessionId);

        recalculated = new AssessmentCandidateResult();
        recalculated.setId(candidateResultId);
        recalculated.setSessionId(sessionId);
        recalculated.setTotalScore(new BigDecimal("7.50"));
        recalculated.setStatus(AssessmentCandidateResultStatus.RELEASED);

        when(assessmentItemEvaluationRepository.findByResponseIdIn(anyCollection())).thenReturn(List.of());
        when(assessmentItemEvaluationRepository.save(any())).thenAnswer(call -> {
            AssessmentItemEvaluation saved = (AssessmentItemEvaluation) call.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(upsertAssessmentCandidateResultUseCase.execute(any(), any())).thenReturn(recalculated);
        when(gradingItemScoreResolver.resolve(any(), any(), anyBoolean())).thenReturn(List.of(
            new ResolvedItem(paperItemId, responseId, new BigDecimal("7.50"), "Tốt hơn phần trước",
                List.of(new SubmitGradingCommand.CriterionScoreItem(
                    criterionId, new BigDecimal("8.00"), "Phát âm rõ")),
                List.of())));
    }

    private AssessmentCandidateResult given(GradingRoundType roundType, AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult current = new AssessmentCandidateResult();
        current.setId(candidateResultId);
        current.setSessionId(sessionId);
        current.setStatus(status);
        current.setTotalScore(new BigDecimal("6.00"));

        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(candidateResultId, teacherId, roundType, null,
            current.getTotalScore(), Instant.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        AssessmentGradingAccessService.GradingContext context = new GradingContext(assignment, current, session, UUID.randomUUID(), "IELTS Mock", AssessmentKind.CENTRALIZED);

        when(gradingActionSupport.prepare(assignmentId, GradingOutcome.REGRADED, null)).thenReturn(
            new PreparedAction(context, teacherId, roundType, GradingOutcome.REGRADED, null,
                new ResultStatusHistoryRecorder.Snapshot(
                    candidateResultId, status, current.getTotalScore())));
        return current;
    }

    private SubmitGradingCommand command() {
        return new SubmitGradingCommand(assignmentId, List.of(new SubmitGradingCommand.ItemGrade(
            paperItemId,
            List.of(new SubmitGradingCommand.CriterionScoreItem(
                criterionId, new BigDecimal("8.00"), "Phát âm rõ")),
            "Tốt hơn phần trước")));
    }

    private AssessmentItemEvaluation captureSavedEvaluation() {
        ArgumentCaptor<AssessmentItemEvaluation> captor = ArgumentCaptor.forClass(AssessmentItemEvaluation.class);
        verify(assessmentItemEvaluationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_write_a_finalized_human_evaluation_for_each_graded_item() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        AssessmentItemEvaluation saved = captureSavedEvaluation();
        assertThat(saved.getEngineType()).isEqualTo(AssessmentEvaluationEngineType.HUMAN);
        assertThat(saved.getStatus()).isEqualTo(AssessmentItemEvaluationStatus.FINALIZED);
        assertThat(saved.getReviewerId()).isEqualTo(teacherId);
        assertThat(saved.getItemScore()).isEqualByComparingTo("7.50");
        assertThat(saved.getPaperItemId()).isEqualTo(paperItemId);
        // Someone who has already graded has nothing left to wait for -- when this flag is on, the
        // calculator holds the submission back.
        assertThat(saved.isRequiresHumanReview()).isFalse();
    }

    @Test
    void should_supersede_every_previous_evaluation_of_the_graded_items() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);
        AssessmentItemEvaluation aiEvaluation = new AssessmentItemEvaluation();
        aiEvaluation.setId(UUID.randomUUID());
        aiEvaluation.setResponseId(responseId);
        aiEvaluation.setStatus(AssessmentItemEvaluationStatus.AUTO_GRADED);
        when(assessmentItemEvaluationRepository.findByResponseIdIn(anyCollection()))
            .thenReturn(List.of(aiEvaluation));

        useCase.execute(command());

        // The calculator picks the effective version by STATUS, and does not prefer HUMAN by
        // engineType -- without this step the old AI score would still be counted into the total.
        assertThat(aiEvaluation.getStatus()).isEqualTo(AssessmentItemEvaluationStatus.SUPERSEDED);
    }

    /**
     * The student's result screen re-reads this very AI version, now SUPERSEDED, to get the
     * speaking turn and the AI evidence (see
     * {@code AssessmentItemEvaluationRepository#findLatestAiByResponseId}). If regrading erased or
     * emptied those columns, the student would lose the question content, audio and transcript the
     * moment the teacher submits -- so this invariant must be locked in here.
     */
    @Test
    void should_only_flip_status_of_the_ai_evaluation_so_its_evidence_survives() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);
        AssessmentItemEvaluation aiEvaluation = new AssessmentItemEvaluation();
        aiEvaluation.setId(UUID.randomUUID());
        aiEvaluation.setResponseId(responseId);
        aiEvaluation.setEngineType(AssessmentEvaluationEngineType.AI_SINGLE);
        aiEvaluation.setStatus(AssessmentItemEvaluationStatus.AUTO_GRADED);
        aiEvaluation.setOverallConfidence(new BigDecimal("0.82"));
        aiEvaluation.setValidityJson("{\"ruleResults\":[]}");
        aiEvaluation.setFeedbackSummary("AI: phát âm ổn");
        aiEvaluation.setSuggestionsJson("[\"nói chậm lại\"]");
        aiEvaluation.setPromptVersion("v3");
        when(assessmentItemEvaluationRepository.findByResponseIdIn(anyCollection()))
            .thenReturn(List.of(aiEvaluation));

        useCase.execute(command());

        assertThat(aiEvaluation.getEngineType()).isEqualTo(AssessmentEvaluationEngineType.AI_SINGLE);
        assertThat(aiEvaluation.getOverallConfidence()).isEqualByComparingTo("0.82");
        assertThat(aiEvaluation.getValidityJson()).isEqualTo("{\"ruleResults\":[]}");
        assertThat(aiEvaluation.getFeedbackSummary()).isEqualTo("AI: phát âm ổn");
        assertThat(aiEvaluation.getSuggestionsJson()).isEqualTo("[\"nói chậm lại\"]");
        assertThat(aiEvaluation.getPromptVersion()).isEqualTo("v3");
    }

    @Test
    void should_not_rewrite_an_evaluation_that_is_already_superseded() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);
        AssessmentItemEvaluation old = new AssessmentItemEvaluation();
        old.setId(UUID.randomUUID());
        old.setResponseId(responseId);
        old.setStatus(AssessmentItemEvaluationStatus.SUPERSEDED);
        when(assessmentItemEvaluationRepository.findByResponseIdIn(anyCollection())).thenReturn(List.of(old));

        useCase.execute(command());

        // Exactly ONE save: the new version. The old version is already SUPERSEDED and is not
        // written again.
        verify(assessmentItemEvaluationRepository).save(any());
    }

    @Test
    void should_store_the_criterion_scores_against_the_new_evaluation() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssessmentItemCriterionScore>> captor = (ArgumentCaptor<List<AssessmentItemCriterionScore>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(assessmentItemCriterionScoreRepository).saveAll(captor.capture());
        List<AssessmentItemCriterionScore> scores = captor.getValue();
        assertThat(scores).hasSize(1);
        assertThat(scores.get(0).getRubricCriterionId()).isEqualTo(criterionId);
        assertThat(scores.get(0).getRawScore()).isEqualByComparingTo("8.00");
        assertThat(scores.get(0).getFinalScore()).isEqualByComparingTo("8.00");
        assertThat(scores.get(0).getEvaluationId()).isNotNull();
    }

    @Test
    void should_keep_a_spot_checked_result_released_instead_of_taking_the_score_back() {
        given(GradingRoundType.SPOT_CHECK, AssessmentCandidateResultStatus.RELEASED);

        useCase.execute(command());

        // resultStatusAfter(SPOT_CHECK, REGRADED) = null means KEEP AS IS. If resolveDefaultStatus
        // were left to decide, a submission with an item that needs checking would be pulled back to
        // PENDING_REVIEW -- i.e. withdrawing a published score for a technical reason.
        verify(upsertAssessmentCandidateResultUseCase).execute(sessionId, AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_release_the_result_after_an_initial_round() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        verify(upsertAssessmentCandidateResultUseCase).execute(sessionId, AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_release_the_result_after_an_appeal_round() {
        given(GradingRoundType.APPEAL, AssessmentCandidateResultStatus.RE_GRADING);

        useCase.execute(command());

        verify(upsertAssessmentCandidateResultUseCase).execute(sessionId, AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_clear_the_flag_once_a_human_has_looked_at_the_session() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);
        session.setFlagged(true);

        useCase.execute(command());

        assertThat(session.isFlagged()).isFalse();
        verify(assessmentSessionRepository).save(session);
    }

    @Test
    void should_not_touch_a_session_that_was_never_flagged() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        verify(assessmentSessionRepository, never()).save(any());
    }

    @Test
    void should_demand_full_coverage_before_closing_the_assignment() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        // Submitting finalizes COMPLETED: if a section is missing and submission is still allowed, that section is locked in for good.
        verify(gradingItemScoreResolver).resolve(any(), any(), eq(true));
    }

    @Test
    void should_finish_with_the_recalculated_result_not_the_stale_one() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        GradingActionResponse response = useCase.execute(command());

        // finish() writes the audit and publishes the event based on the score -- passing the old
        // version in would report the wrong score to the student.
        verify(gradingActionSupport).finish(any(), eq(recalculated));
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
        assertThat(response.outcome()).isEqualTo(GradingOutcome.REGRADED.name());
    }

    @Test
    void should_save_criterion_scores_even_when_nothing_needed_superseding() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        verify(assessmentItemCriterionScoreRepository).saveAll(anyList());
    }
}
