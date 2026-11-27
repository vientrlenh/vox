package com.sep.vox.infrastructure.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Checks for the ORPHANED ROW case: an attempt carrying a code that the gateway has never seen.
 *
 * <p>This state only exists since CreatePaymentCheckoutUrlUseCase commits the PENDING row BEFORE calling
 * createPaymentLink -- if that call fails midway the row stays, while the gateway has no session.
 *
 * <p>Why it deserves its own test: if this job does NOT settle the orphaned row then {@code expireIfOverdue} refuses to
 * close the order (it sees a pending attempt), so the order is stuck PENDING FOREVER and not just until expiry
 * -- expiring is exactly what is being blocked. For a subscription order,
 * uq_orders_one_open_subscription_order also locks the school out of placing a new order, and the manual
 * escape path (CancelOrderUseCase) goes through exactly the same decision. Breaking here means breaking into something that has to be fixed
 * by hand in the DB.
 */
class PendingOrderReconcilerTests {

    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderRepository orderRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private PaymentProcessPort paymentProcessPort;
    private OrderSettlementService orderSettlementService;
    private PendingOrderReconciler reconciler;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        paymentProcessPort = mock(PaymentProcessPort.class);
        orderSettlementService = mock(OrderSettlementService.class);

        when(paymentProcessPort.provider()).thenReturn(PaymentProvider.PAYOS);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(overdueOrder()));

        reconciler = new PendingOrderReconciler(
            orderRepository, paymentRecordRepository,
            new PaymentProcessResolver(List.of(paymentProcessPort)),
            orderSettlementService);
    }

    @Test
    void should_close_an_attempt_the_gateway_has_never_heard_of() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.NOT_FOUND);

        reconciler.reconcile();

        verify(orderSettlementService).failAttempt(any(), eq(PaymentStatus.FAILED));
    }

    /**
     * The other side: for an attempt opened only a few seconds ago, NOT_FOUND does NOT mean it is dead -- most likely
     * createPaymentLink is still in flight and the gateway has not yet built the session. A background job is the thing most likely to land in exactly that
     * window, and settling wrongly here means a school pays into a real link while the row that corresponds
     * to that payment already carries FAILED.
     */
    @Test
    void should_leave_a_freshly_opened_attempt_alone_when_the_gateway_says_not_found() {
        givenPendingAttempt(Instant.now().minus(3, ChronoUnit.SECONDS), PaymentLinkRemoteStatus.NOT_FOUND);

        reconciler.reconcile();

        verify(orderSettlementService, never()).failAttempt(any(), any());
    }

    /** The gateway says the session is still waiting for payment: touch nothing, ask again in the next run. */
    @Test
    void should_leave_a_live_attempt_alone() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.PENDING);

        reconciler.reconcile();

        verify(orderSettlementService, never()).failAttempt(any(), any());
        verify(orderSettlementService, never()).settlePaid(any(), any());
    }

    private void givenPendingAttempt(Instant createdAt, PaymentLinkRemoteStatus remoteStatus) {
        PaymentRecord attempt = PaymentRecord.forEBankingCheckout(
            ORDER_ID, new BigDecimal("500000"), PaymentProvider.PAYOS, "orphan-ref", createdAt);
        attempt.setId(UUID.randomUUID());
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(attempt));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(remoteStatus));
    }

    private static Order overdueOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setSchoolId(UUID.randomUUID());
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmountVnd(new BigDecimal("500000"));
        order.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return order;
    }
}
