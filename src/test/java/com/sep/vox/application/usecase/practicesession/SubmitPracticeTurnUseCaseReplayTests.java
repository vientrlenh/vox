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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.sep.vox.application.port.input.service.ConsumeQuotaService;
import com.sep.vox.application.port.input.service.PracticeEvaluationRequestFactory;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.practicesession.SubmitPracticeTurnUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
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
 * Python gọi /internal/practice-sessions/{id}/turns và có thể GỬI LẠI đúng một lượt khi mất response
 * HTTP -- PracticeResponseTurnRepositoryImpl.save đã dựng riêng để chịu được điều đó.
 *
 * <p>Nhưng bảng lượt nói là thứ DUY NHẤT idempotent trong luồng này. Ba việc chạy sau nó thì không, và
 * cả ba đều tốn tiền hoặc bịa dữ liệu nếu chạy hai lần:
 * <ul>
 *   <li>trừ chi phí AI vào ví trường -- và sổ cái vẫn CÂN sau khi trừ đôi (bút toán lẫn số dư cùng
 *       bị trừ), nên bất biến SUM(entries.amount_vnd) = balance_vnd không bắt được;</li>
 *   <li>ghi dòng sửa lỗi -- turn_corrections không có ràng buộc duy nhất nào;</li>
 *   <li>bắn yêu cầu chấm sang Kafka -- một lượt chấm AI thứ hai cho cùng một câu trả lời.</li>
 * </ul>
 *
 * <p>Bộ test này giữ đúng chỗ đó: lượt MỚI làm đủ ba việc, lượt GỬI LẠI không làm việc nào.
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

        var schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        var evaluationRequestFactory = mock(PracticeEvaluationRequestFactory.class);
        var enrichmentService = mock(PracticeTopicOfferEnrichmentService.class);
        var userContextPort = mock(UserContextPort.class);
        var quotaPricingPort = mock(QuotaPricingPort.class);
        var transactionManager = mock(PlatformTransactionManager.class);

        // TransactionTemplate thật chạy trên manager giả: getTransaction -> chạy callback -> commit.
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

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
            transactionManager
        );
    }

    @Test
    void should_charge_store_corrections_and_queue_evaluation_on_a_new_turn() {
        givenTurnWrite(true);

        useCase.execute(STUDENT_ID, turn());

        verify(consumeQuotaService, times(1))
            .consumePracticeAllowingDebt(any(), any(), any(), any(), any(), any());
        verify(turnCorrectionRepository, times(1)).save(any(), any(), any(), any(), any(), any());
        verify(practiceSessionRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publish(any());
    }

    /**
     * Cùng một lượt gửi lại: bảng lượt nói trả về dòng cũ (created=false) và KHÔNG có việc nào trong ba
     * việc kia được chạy lần thứ hai. Đây là test giữ cho tiền thật không bị trừ đôi.
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
     * Bỏ GHI không có nghĩa là bỏ TRẢ VỀ: client gửi lại vì chưa nhận được kết quả lần đầu, nên nó vẫn
     * phải nhận đủ danh sách sửa lỗi -- payload retry chính là payload lần đầu.
     */
    @Test
    void should_still_return_corrections_on_a_resend() {
        givenTurnWrite(false);

        var result = useCase.execute(STUDENT_ID, turn());

        assertThat(result.corrections()).hasSize(1);
    }

    private void givenTurnWrite(boolean created) {
        when(practiceResponseTurnRepository.save(any(), anyInt(), any(), any(), any(), any(),
            anyInt(), any(), any()))
            .thenReturn(new PracticeResponseTurnRepository.TurnWrite(TURN_ID, created));
    }

    private SubmitPracticeTurn turn() {
        var turn = new SubmitPracticeTurn();
        turn.setSessionId(SESSION_ID);
        turn.setQuestionId(QUESTION_ID);
        turn.setTurnOrder(1);
        turn.setTurnType("ANSWER");
        turn.setTranscript("I go to school yesterday.");
        // > 0 để đi vào nhánh trừ tiền, và questionComplete để đi vào nhánh bắn yêu cầu chấm.
        turn.setDurationSeconds(30);
        turn.setTurnCostUsd(new BigDecimal("0.0125"));
        turn.setQuestionComplete(true);
        turn.setCorrections(List.of(new TurnCorrectionSubmission(
            "grammar", "I go to school yesterday", "I went to school yesterday",
            "Thì quá khứ đơn", null, 0.95)));
        return turn;
    }
}
