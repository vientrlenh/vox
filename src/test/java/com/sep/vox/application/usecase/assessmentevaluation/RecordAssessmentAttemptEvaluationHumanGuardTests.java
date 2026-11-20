package com.sep.vox.application.usecase.assessmentevaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.sep.vox.application.port.input.command.assessmentevaluation.RecordAssessmentAttemptEvaluationCommand;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.RecordAssessmentAttemptEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CompleteAssessmentSessionGradingUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
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
import com.sep.vox.interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedPayloadDto;
import com.sep.vox.interfaces.kafka.mapper.RecordAssessmentAttemptEvaluationCommandMapper;

/**
 * A score finalized by a person must beat the AI.
 *
 * <p>There is no optimistic lock on assessment_item_evaluations: if the consumer kept overwriting,
 * a regrading run by the AI (re-run or Kafka replay) would swallow the teacher's HUMAN
 * FINALIZED version and wipe the criterion scores too -- a silent loss that nobody would notice. This is the test that reproduces exactly that scenario.
 */
public class RecordAssessmentAttemptEvaluationHumanGuardTests {

    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private AssessmentItemEvaluationTurnRepository assessmentItemEvaluationTurnRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentPolicyRepository assessmentPolicyRepository;
    private RubricVersionRepository rubricVersionRepository;
    private AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;
    private UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
    private CompleteAssessmentSessionGradingUseCase completeAssessmentSessionGradingUseCase;
    private JsonSerializationPort jsonSerializationPort;
    private RecordAssessmentAttemptEvaluationUseCase useCase;

    private final UUID responseId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        assessmentItemCriterionScoreRepository = mock(AssessmentItemCriterionScoreRepository.class);
        assessmentItemEvaluationTurnRepository = mock(AssessmentItemEvaluationTurnRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentProctoringAlertRepository = mock(AssessmentProctoringAlertRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        upsertAssessmentCandidateResultUseCase = mock(UpsertAssessmentCandidateResultUseCase.class);
        completeAssessmentSessionGradingUseCase = mock(CompleteAssessmentSessionGradingUseCase.class);
        jsonSerializationPort = mock(JsonSerializationPort.class);

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenAnswer(
            invocation -> (TransactionStatus) new SimpleTransactionStatus());

        useCase = new RecordAssessmentAttemptEvaluationUseCase(
            assessmentItemResponseRepository, assessmentItemEvaluationRepository, assessmentItemCriterionScoreRepository,
            assessmentItemEvaluationTurnRepository, rubricCriterionRepository, assessmentSessionRepository, assessmentRepository,
            assessmentPolicyRepository, rubricVersionRepository, upsertAssessmentCandidateResultUseCase,
            completeAssessmentSessionGradingUseCase,
            transactionManager, jsonSerializationPort, new ConfidenceReviewCalculator(),
            mock(ClassTestGradingAssignmentService.class), assessmentProctoringAlertRepository);

        // Default: NO serious proctoring alert. The tests here check the override-blocking rule and
        // the confidence rule, so the proctoring variable must be removed from the result entirely
        // -- a mock that returns false by Mockito's default gives the same outcome, but saying it
        // explicitly spares the reader from guessing.
        when(assessmentProctoringAlertRepository.hasIntegrityAlert(any())).thenReturn(false);

        when(assessmentItemResponseRepository.findById(responseId)).thenReturn(Optional.of(
            new AssessmentItemResponse(responseId, sessionId, paperItemId, null, null, null, null, null)));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of());

        // The session -> assessment -> policy -> rubric chain is only needed for the branches that are
        // NOT blocked; a blocked branch must exit before touching it.
        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setAssessmentId(assessmentId);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setAssessmentPolicyId(policyId);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        AssessmentPolicy policy = new AssessmentPolicy();
        policy.setId(policyId);
        policy.setRubricVersionId(rubricVersionId);
        when(assessmentPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(rubricVersionRepository.findById(rubricVersionId))
            .thenReturn(Optional.of(new com.sep.vox.domain.model.rubric.RubricVersion()));
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of());
    }

    private RecordAssessmentAttemptEvaluationCommand command() {
        AssessmentAttemptEvaluationCompletedPayloadDto payload = new AssessmentAttemptEvaluationCompletedPayloadDto(
            List.of(), Map.of(), null, null, "Khá", List.of(), "gpt-4o", "v1", null);
        AssessmentAttemptEvaluationCompletedEventDto event = new AssessmentAttemptEvaluationCompletedEventDto(
            "assessment.attempt.evaluation.completed", 1, null, responseId.toString(), null, payload);
        return RecordAssessmentAttemptEvaluationCommandMapper.toCommand(event);
    }

    private void givenLatestEvaluation(
            AssessmentEvaluationEngineType engineType, AssessmentItemEvaluationStatus status) {
        AssessmentItemEvaluation evaluation = new AssessmentItemEvaluation();
        evaluation.setId(UUID.randomUUID());
        evaluation.setResponseId(responseId);
        evaluation.setEngineType(engineType);
        evaluation.setStatus(status);
        evaluation.setItemScore(new BigDecimal("8.00"));
        when(assessmentItemEvaluationRepository.findLatestByResponseId(responseId))
            .thenReturn(Optional.of(evaluation));
    }

    @Test
    void should_skip_ai_regrade_when_a_human_evaluation_is_already_finalized() {
        givenLatestEvaluation(AssessmentEvaluationEngineType.HUMAN, AssessmentItemEvaluationStatus.FINALIZED);

        useCase.execute(command());

        verify(assessmentItemEvaluationRepository, never()).save(any());
        verify(assessmentItemCriterionScoreRepository, never()).deleteByEvaluationIdIn(anyCollection());
        verify(assessmentItemEvaluationTurnRepository, never()).deleteByEvaluationIdIn(anyCollection());
    }

    @Test
    void should_still_overwrite_a_previous_ai_evaluation() {
        givenLatestEvaluation(AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.AUTO_GRADED);
        when(assessmentItemEvaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        // Chỉ điểm NGƯỜI mới được bảo vệ; AI chấm lại chính bản AI của nó là bình thường.
        verify(assessmentItemEvaluationRepository).save(any());
    }

    @Test
    void should_still_record_the_first_ai_evaluation() {
        when(assessmentItemEvaluationRepository.findLatestByResponseId(responseId)).thenReturn(Optional.empty());
        when(assessmentItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            AssessmentItemEvaluation evaluation = (AssessmentItemEvaluation) invocation.getArgument(0);
            evaluation.setId(UUID.randomUUID());
            return evaluation;
        });

        useCase.execute(command());

        verify(assessmentItemEvaluationRepository).save(any());
    }

    @Test
    void should_keep_the_stored_human_score_untouched() {
        givenLatestEvaluation(AssessmentEvaluationEngineType.HUMAN, AssessmentItemEvaluationStatus.FINALIZED);

        useCase.execute(command());

        AssessmentItemEvaluation stored = assessmentItemEvaluationRepository.findLatestByResponseId(responseId).orElseThrow();
        assertThat(stored.getItemScore()).isEqualByComparingTo("8.00");
        assertThat(stored.getStatus()).isEqualTo(AssessmentItemEvaluationStatus.FINALIZED);
    }

    /**
     * A proctoring alert that raises an integrity suspicion pushes the submission to a reviewer,
     * regardless of the confidence score.
     *
     * <p>The payload used here is a normal payload -- no validity flag, no bad signal -- so without
     * the proctoring rule {@code requiresHumanReview} would be {@code false}. That is exactly the
     * condition that lets this test prove the new rule, instead of being right for some other
     * reason.
     *
     * <p>This test mocks the repository directly, so it does NOT pin down which alert types belong
     * to the integrity group -- and that is exactly why lowering the level of MULTIPLE_PERSONS
     * passes through here without turning anything red. That part lives in
     * {@code AssessmentProctoringAlertIntegrityTypesTests}.
     */
    @Test
    void should_require_human_review_when_the_session_has_an_integrity_proctoring_alert() {
        when(assessmentProctoringAlertRepository.hasIntegrityAlert(sessionId)).thenReturn(true);
        when(assessmentItemEvaluationRepository.findLatestByResponseId(responseId)).thenReturn(Optional.empty());
        AtomicReference<AssessmentItemEvaluation> saved = new java.util.concurrent.atomic.AtomicReference<AssessmentItemEvaluation>();
        when(assessmentItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            AssessmentItemEvaluation evaluation = (AssessmentItemEvaluation) invocation.getArgument(0);
            evaluation.setId(UUID.randomUUID());
            saved.set(evaluation);
            return evaluation;
        });

        useCase.execute(command());

        assertThat(saved.get().isRequiresHumanReview()).isTrue();
        assertThat(saved.get().getReviewReasonCode()).isEqualTo("PROCTORING_INTEGRITY");
    }

    /** Without an integrity alert, this rule must not switch itself on. */
    @Test
    void should_not_require_human_review_when_there_is_no_integrity_proctoring_alert() {
        when(assessmentProctoringAlertRepository.hasIntegrityAlert(sessionId)).thenReturn(false);
        when(assessmentItemEvaluationRepository.findLatestByResponseId(responseId)).thenReturn(Optional.empty());
        AtomicReference<AssessmentItemEvaluation> saved = new java.util.concurrent.atomic.AtomicReference<AssessmentItemEvaluation>();
        when(assessmentItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            AssessmentItemEvaluation evaluation = (AssessmentItemEvaluation) invocation.getArgument(0);
            evaluation.setId(UUID.randomUUID());
            saved.set(evaluation);
            return evaluation;
        });

        useCase.execute(command());

        assertThat(saved.get().getReviewReasonCode()).isNotEqualTo("PROCTORING_INTEGRITY");
    }
}
