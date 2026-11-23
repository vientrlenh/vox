package com.sep.vox.application.usecase.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.sep.vox.application.port.input.command.order.CancelOrderCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.order.CancelOrderUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.port.output.UserContextPort;
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
 * Đường thoát THỦ CÔNG khi trường đặt nhầm đơn. Nó là chốt cuối: nếu nó không đóng được đơn thì chỉ
 * còn sửa tay dưới DB, vì đơn đăng ký còn mở chặn trường đặt đơn mới
 * (uq_orders_one_open_subscription_order).
 *
 * <p>Quy tắc xuyên suốt: chỉ đóng đơn khi CHẮC CHẮN không còn đường nào ra tiền. Nên hai phía đều
 * phải kiểm -- không được đóng khi phiên còn sống, và không được từ chối khi phiên đã chết.
 */
class CancelOrderUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderRepository orderRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private PaymentProcessPort paymentProcessPort;
    private CancelOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        paymentProcessPort = mock(PaymentProcessPort.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        when(paymentProcessPort.provider()).thenReturn(PaymentProvider.PAYOS);
        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        useCase = new CancelOrderUseCase(
            orderRepository, paymentRecordRepository,
            new PaymentProcessResolver(List.of(paymentProcessPort)),
            userContextPort);
    }

    /**
     * An orphan row (the gateway has never seen the code) must NOT fall into the "session is still
     * alive" branch: cancelling a gateway code it does not know makes cancelPaymentLink return
     * false, and the school gets the message "wait for the order to expire" -- while that very
     * orphan row is what blocks expireIfOverdue, so the order would never expire.
     */
    @Test
    void should_cancel_when_the_gateway_has_never_heard_of_the_attempt() {
        PaymentRecord orphan = givenPendingAttempt(
            Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.NOT_FOUND);

        useCase.execute(new CancelOrderCommand(ORDER_ID));

        assertThat(orphan.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentProcessPort, never()).cancelPaymentLink(anyString(), anyString());
        verify(orderRepository).save(any());
    }

    /**
     * The attempt was opened only a few seconds ago: NOT_FOUND right now is very likely just
     * createPaymentLink still in flight. Refusing the cancellation is CORRECT -- but the school
     * must be told to simply retry in a few minutes, not "wait for the order to expire".
     */
    @Test
    void should_refuse_to_cancel_an_attempt_that_was_just_opened() {
        PaymentRecord justOpened = givenPendingAttempt(
            Instant.now().minus(3, ChronoUnit.SECONDS), PaymentLinkRemoteStatus.NOT_FOUND);

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(ORDER_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thử lại sau vài phút");

        assertThat(justOpened.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    /** Cổng nói đã thu tiền: tuyệt đối không đóng đơn, nếu không là thu tiền xong không giao gì. */
    @Test
    void should_refuse_to_cancel_a_paid_order() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.PAID);

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(ORDER_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã được thanh toán");

        verify(orderRepository, never()).save(any());
    }

    /** Phiên còn sống mà cổng không cho hủy (SePay luôn rơi vào đây): không đóng đơn. */
    @Test
    void should_refuse_to_cancel_when_a_live_session_cannot_be_closed() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.PENDING);
        when(paymentProcessPort.cancelPaymentLink(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(ORDER_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cổng không cho hủy sớm");

        verify(orderRepository, never()).save(any());
    }

    private PaymentRecord givenPendingAttempt(Instant createdAt, PaymentLinkRemoteStatus remoteStatus) {
        PaymentRecord attempt = PaymentRecord.forEBankingCheckout(
            ORDER_ID, new BigDecimal("500000"), PaymentProvider.PAYOS, "orphan-ref", createdAt);
        attempt.setId(UUID.randomUUID());
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(attempt));
        when(paymentRecordRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(remoteStatus));
        return attempt;
    }

    private static Order pendingOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setSchoolId(SCHOOL_ID);
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmountVnd(new BigDecimal("500000"));
        order.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        return order;
    }
}
