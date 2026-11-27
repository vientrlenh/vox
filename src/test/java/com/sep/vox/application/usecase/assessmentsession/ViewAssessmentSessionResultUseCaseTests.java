package com.sep.vox.application.usecase.assessmentsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.assessmentsession.ViewAssessmentSessionResultQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService.SessionAccess;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator.ItemScore;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator.SectionScore;
import com.sep.vox.application.port.input.usecase.assessmentsession.ViewAssessmentSessionResultUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentCandidateResultResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * The only door through which a student views their own result, so this is where two invariants are locked:
 * a submission with no conclusion does not reveal the score to its owner, while a teacher/admin can always
 * see it -- they need the unpublished score as a basis for grading.
 *
 * <p>The page must still open when masked: if it threw an error the student would only see "not found"
 * and would not know where their submission stands.
 */
class ViewAssessmentSessionResultUseCaseTests {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.randomUUID();
    private static final UUID PAPER_ITEM_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID RESPONSE_ID = UUID.randomUUID();

    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentSessionResultCalculator assessmentSessionResultCalculator;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private RubricResultBandRepository rubricResultBandRepository;
    private RubricVersionRepository rubricVersionRepository;
    private AssessmentResultAccessService assessmentResultAccessService;
    private QuestionRepository questionRepository;
    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private com.sep.vox.domain.repository.QuestionAssetRepository questionAssetRepository;
    private ViewAssessmentSessionResultUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentSessionResultCalculator = mock(AssessmentSessionResultCalculator.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        rubricResultBandRepository = mock(RubricResultBandRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        assessmentResultAccessService = mock(AssessmentResultAccessService.class);
        questionRepository = mock(QuestionRepository.class);
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        questionAssetRepository = mock(com.sep.vox.domain.repository.QuestionAssetRepository.class);
        // By default a question has NO asset: every existing test builds a bare question, and returning empty keeps
        // the behavior they assert.
        when(questionAssetRepository.findByQuestionId(any())).thenReturn(java.util.List.of());
        useCase = new ViewAssessmentSessionResultUseCase(
            assessmentCandidateResultRepository,
            assessmentSessionResultCalculator,
            frameworkResultBandRepository,
            rubricResultBandRepository,
            rubricVersionRepository,
            assessmentResultAccessService,
            questionRepository,
            assessmentItemResponseRepository,
            assessmentItemEvaluationRepository,
            assessmentPaperItemRepository,
            questionAssetRepository
        );
    }

    @Test
    void should_hide_score_from_candidate_when_result_pending_review() {
        givenAccess(true, false);
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isFalse();
        assertThat(response.totalScore()).isNull();
        assertThat(response.rubricResultBandId()).isNull();
        assertThat(response.sections()).isEmpty();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void should_still_return_status_when_score_hidden() {
        givenAccess(true, false);
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        // The page must open so the student knows the submission is waiting for grading, instead of
        // "not found".
        assertThat(response.status()).isEqualTo("PENDING_REVIEW");
        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void should_not_call_calculator_when_score_hidden() {
        givenAccess(true, false);
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        verify(assessmentSessionResultCalculator, never()).calculate(any());
    }

    @Test
    void should_show_score_to_candidate_when_result_released() {
        givenAccess(true, false);
        givenResult(AssessmentCandidateResultStatus.RELEASED);
        givenCalculatedBreakdown();

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
        assertThat(response.sections()).hasSize(1);
        assertThat(response.items()).hasSize(1);
    }

    /**
     * The old predicate only allowed FINAL/RELEASED, so a submission that was flagged and then
     * finalized as PASSED was hidden from its own owner -- a latent bug fixed in the same batch.
     */
    @Test
    void should_show_score_to_candidate_when_flagged_result_passed() {
        givenAccess(true, true);
        givenResult(AssessmentCandidateResultStatus.PASSED);
        givenCalculatedBreakdown();

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_show_score_to_teacher_when_result_pending_review() {
        givenAccess(false, false);
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);
        givenCalculatedBreakdown();

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void should_hide_breakdown_but_keep_total_when_result_invalid() {
        givenAccess(true, false);
        givenResult(AssessmentCandidateResultStatus.INVALID);

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.sections()).isEmpty();
        assertThat(response.items()).isEmpty();
        verify(assessmentSessionResultCalculator, never()).calculate(any());
    }

    @Test
    void should_hide_score_from_candidate_when_result_re_grading() {
        givenAccess(true, false);
        givenResult(AssessmentCandidateResultStatus.RE_GRADING);

        AssessmentCandidateResultResponse response = useCase.execute(new ViewAssessmentSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isFalse();
        assertThat(response.status()).isEqualTo("RE_GRADING");
    }

    private void givenAccess(boolean candidateOwner, boolean flagged) {
        AssessmentSession session = AssessmentSession.builder()
                .id(SESSION_ID)
                .assessmentId(ASSESSMENT_ID)
                .candidateId(CANDIDATE_ID)
                .paperId(PAPER_ID)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .status(AssessmentSessionStatus.GRADED)
                .flagged(flagged)
                .flagReason(flagged ? "nghi vấn" : null)
                .build();
        when(assessmentResultAccessService.authorizeSession(SESSION_ID))
            .thenReturn(new SessionAccess(session, candidateOwner));
    }

    private void givenResult(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = AssessmentCandidateResult.builder()
                .id(UUID.randomUUID())
                .assessmentId(ASSESSMENT_ID)
                .candidateId(CANDIDATE_ID)
                .sessionId(SESSION_ID)
                .assessmentPolicyId(UUID.randomUUID())
                .policyVersion(1)
                .rubricVersionId(UUID.randomUUID())
                .frameworkVersionId(UUID.randomUUID())
                .targetFrameworkBandId(null)
                .rubricResultBandId(null)
                .totalScore(new BigDecimal("7.50"))
                .status(status)
                .releasedAt(null)
                .finalizedAt(null)
                .build();
        when(assessmentCandidateResultRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(result));
    }

    private void givenCalculatedBreakdown() {
        // shouldIncludeBreakdown now asks "does every item have a graded version" instead of only
        // looking at the status, so a submission with a score breakdown must have a response AND a
        // matching evaluation.
        AssessmentItemResponse response = mock(com.sep.vox.domain.model.assessment.AssessmentItemResponse.class);
        when(response.getId()).thenReturn(RESPONSE_ID);
        when(assessmentItemResponseRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(response));
        AssessmentItemEvaluation evaluation = mock(com.sep.vox.domain.model.assessment.AssessmentItemEvaluation.class);
        when(evaluation.getResponseId()).thenReturn(RESPONSE_ID);
        when(assessmentItemEvaluationRepository.findByResponseIdIn(List.of(RESPONSE_ID)))
            .thenReturn(List.of(evaluation));
        when(assessmentSessionResultCalculator.calculate(SESSION_ID)).thenReturn(new CalculatedAssessmentSessionResult(
            SESSION_ID, ASSESSMENT_ID, PAPER_ID, CANDIDATE_ID, null, new BigDecimal("7.50"), null, null,
            List.of(new SectionScore(SECTION_ID, "Part 1", new BigDecimal("7.50"))),
            List.of(new ItemScore(PAPER_ITEM_ID, RESPONSE_ID, SECTION_ID, QUESTION_ID,
                new BigDecimal("7.50"), new BigDecimal("7.50"))),
            false));
    }
}
