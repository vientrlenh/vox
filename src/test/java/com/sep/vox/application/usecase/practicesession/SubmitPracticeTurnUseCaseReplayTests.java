package com.sep.vox.application.usecase.practicesession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ConsumeQuotaService;
import com.sep.vox.application.port.input.service.PracticeEvaluationRequestFactory;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.practicesession.SubmitPracticeTurnUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses;
import com.sep.vox.application.response.input.subscription.ConsumeQuotaResponse;
import com.sep.vox.domain.model.personalization.PracticeSession;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.TurnCorrectionRepository;

/**
 * Python calls /internal/practice-sessions/{id}/turns and may RESEND exactly one turn when the HTTP response is lost --
 * PracticeResponseTurnRepositoryImpl.save was built specifically to tolerate that.
 *
 * <p>But the speaking-turn table is the ONLY idempotent thing in this flow. The three steps that run after it are
 * not, and all three cost money or invent data if run twice:
 * <ul>
 *   <li>charging the AI cost to the school balance -- and the ledger still BALANCES after a double charge (the
 *       entry and the balance are both deducted), so the invariant SUM(entries.amount_vnd) = balance_vnd does not
 *       catch it;</li>
 *   <li>writing the correction lines -- turn_corrections has no unique constraint at all;</li>
 *   <li>firing the grading request to Kafka -- a second AI grading of the same answer.</li>
 * </ul>
 *
 * <p>This test suite guards exactly that: a NEW turn does all three, a RESENT turn does none of them.
 */
class SubmitPracticeTurnUseCaseReplayTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final UUID TURN_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    private PracticeSessionRepository practiceSessionRepository;
    private PracticeItemResponseRepository practiceItemResponseRepository;
    private PracticeResponseTurnRepository practiceResponseTurnRepository;
    private TurnCorrectionRepository turnCorrectionRepository;
    private ConsumeQuotaService consumeQuotaService;
    private RequiresNewTransactionPort requiresNewTransactionPort;
    private ExternalEventPublisherPort eventPublisher;
    private SubmitPracticeTurnUseCase useCase;

    @BeforeEach
    void setUp() {
        practiceSessionRepository = mock(PracticeSessionRepository.class);
        practiceItemResponseRepository = mock(PracticeItemResponseRepository.class);
        practiceResponseTurnRepository = mock(PracticeResponseTurnRepository.class);
        turnCorrectionRepository = mock(TurnCorrectionRepository.class);
        consumeQuotaService = mock(ConsumeQuotaService.class);
        eventPublisher = mock(ExternalEventPublisherPort.class);

        SchoolSubscriptionRepository schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        PracticeEvaluationRequestFactory evaluationRequestFactory = mock(PracticeEvaluationRequestFactory.class);
        PracticeTopicOfferEnrichmentService enrichmentService = mock(PracticeTopicOfferEnrichmentService.class);
        UserContextPort userContextPort = mock(UserContextPort.class);
        QuotaPricingPort quotaPricingPort = mock(QuotaPricingPort.class);
        requiresNewTransactionPort = mock(RequiresNewTransactionPort.class);

        // The port runs the supplied work straight away, as a committed REQUIRES_NEW transaction would.
        when(requiresNewTransactionPort.callInNewTransaction(any())).thenAnswer(
            invocation -> invocation.<Supplier<?>>getArgument(0).get());

        when(practiceSessionRepository.existsByIdAndStudentIdAndStatus(SESSION_ID, STUDENT_ID, "IN_PROGRESS"))
            .thenReturn(true);
        when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(new PracticeSession()));
        when(practiceItemResponseRepository.upsertResponse(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(RESPONSE_ID);
        when(schoolSubscriptionRepository.findActiveSubscriptionIdForUser(STUDENT_ID))
            .thenReturn(Optional.of(SUBSCRIPTION_ID));
        when(quotaPricingPort.usdToVndRate()).thenReturn(BigDecimal.valueOf(26_000));
        when(consumeQuotaService.consumePracticeAllowingDebt(any(), any(), any(), any(), any(), any()))
            .thenReturn(new ConsumeQuotaResponse(null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, false, false));

        useCase = new SubmitPracticeTurnUseCase(
            practiceSessionRepository,
            practiceItemResponseRepository,
            practiceResponseTurnRepository,
            turnCorrectionRepository,
            schoolSubscriptionRepository,
            evaluationRequestFactory,
            enrichmentService,
            consumeQuotaService,
            eventPublisher,
            userContextPort,
            quotaPricingPort,
            requiresNewTransactionPort
        );
    }

    @Test
    void should_charge_store_corrections_and_queue_evaluation_on_a_new_turn() {
        givenTurnWrite(true);

        useCase.execute(STUDENT_ID, turn());

        verify(requiresNewTransactionPort, times(1)).callInNewTransaction(any());
        verify(consumeQuotaService, times(1))
            .consumePracticeAllowingDebt(any(), any(), any(), any(), any(), any());
        verify(turnCorrectionRepository, times(1)).save(any(), any(), any(), any(), any(), any());
        verify(practiceSessionRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publish(any());
    }

    /**
     * The same turn sent again: the speaking-turn table returns the old row (created=false) and NONE of the three
     * other steps runs a second time. This is the test that keeps real money from being charged twice.
     */
    @Test
    void should_do_nothing_billable_when_python_resends_the_same_turn() {
        givenTurnWrite(false);

        useCase.execute(STUDENT_ID, turn());

        verify(consumeQuotaService, never())
            .consumePracticeAllowingDebt(any(), any(), any(), any(), any(), any());
        verify(turnCorrectionRepository, never()).save(any(), any(), any(), any(), any(), any());
        verify(practiceSessionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    /**
     * Skipping the WRITE does not mean skipping the RETURN: the client resends because it did not
     * receive the result the first time, so it must still get the full list of corrections -- the
     * retry payload is exactly the first payload.
     */
    @Test
    void should_still_return_corrections_on_a_resend() {
        givenTurnWrite(false);

        PracticeSessionResponses.SubmitTurnResult result = useCase.execute(STUDENT_ID, turn());

        assertThat(result.corrections()).hasSize(1);
    }

    private void givenTurnWrite(boolean created) {
        when(practiceResponseTurnRepository.save(any(), anyInt(), any(), any(), any(), any(),
            anyInt(), any(), any()))
            .thenReturn(new PracticeResponseTurnRepository.TurnWrite(TURN_ID, created));
    }

    private SubmitPracticeTurn turn() {
        SubmitPracticeTurn turn = new SubmitPracticeTurn();
        turn.setSessionId(SESSION_ID);
        turn.setQuestionId(QUESTION_ID);
        turn.setTurnOrder(1);
        turn.setTurnType("ANSWER");
        turn.setTranscript("I go to school yesterday.");
        // > 0 to enter the charging branch, and questionComplete to enter the branch that fires the
        // grading request.
        turn.setDurationSeconds(30);
        turn.setTurnCostUsd(new BigDecimal("0.0125"));
        turn.setQuestionComplete(true);
        turn.setCorrections(List.of(TurnCorrectionSubmission.builder()
                .category("grammar")
                .originalText("I go to school yesterday")
                .correctedText("I went to school yesterday")
                .explanation("Thì quá khứ đơn")
                .correctAudioUrl(null)
                .confidence(0.95)
                .build()));
        return turn;
    }
}
