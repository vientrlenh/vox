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

import com.sep.vox.application.port.input.command.CancelOrderCommand;
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
        var userContextPort = mock(UserContextPort.class);

        when(paymentProcessPort.provider()).thenReturn(PaymentProvider.PAYOS);
        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        useCase = new CancelOrderUseCase(
            orderRepository, paymentRecordRepository,
            new PaymentProcessResolver(List.of(paymentProcessPort)),
            userContextPort);
    }

    /**
     * Dòng mồ côi (cổng chưa từng thấy mã) KHÔNG được rơi xuống nhánh "phiên vẫn sống": gọi hủy một
     * mã cổng không biết thì cancelPaymentLink trả false, trường nhận câu "đợi đơn hết hạn" -- trong
     * khi chính dòng mồ côi ấy đang chặn expireIfOverdue, nên đơn sẽ không bao giờ hết hạn.
     */
    @Test
    void should_cancel_when_the_gateway_has_never_heard_of_the_attempt() {
        var orphan = givenPendingAttempt(
            Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.NOT_FOUND);

        useCase.execute(new CancelOrderCommand(ORDER_ID));

        assertThat(orphan.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentProcessPort, never()).cancelPaymentLink(anyString(), anyString());
        verify(orderRepository).save(any());
    }

    /**
     * Lần thử vừa mở vài giây trước: NOT_FOUND lúc này rất có thể chỉ là createPaymentLink còn đang
     * bay. Từ chối hủy là ĐÚNG -- nhưng phải nói cho trường biết chỉ cần thử lại sau vài phút, chứ
     * không phải "đợi đơn hết hạn".
     */
    @Test
    void should_refuse_to_cancel_an_attempt_that_was_just_opened() {
        var justOpened = givenPendingAttempt(
            Instant.now().minus(3, ChronoUnit.SECONDS), PaymentLinkRemoteStatus.NOT_FOUND);

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(ORDER_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("thử lại sau vài phút");

        assertThat(justOpened.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    /** Cổng nói đã thu tiền: tuyệt đối không đóng đơn, nếu không là thu tiền xong không giao gì. */
    @Test
    void should_refuse_to_cancel_a_paid_order() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.PAID);

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(ORDER_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã được thanh toán");

        verify(orderRepository, never()).save(any());
    }

    /** Phiên còn sống mà cổng không cho hủy (SePay luôn rơi vào đây): không đóng đơn. */
    @Test
    void should_refuse_to_cancel_when_a_live_session_cannot_be_closed() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.PENDING);
        when(paymentProcessPort.cancelPaymentLink(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new CancelOrderCommand(ORDER_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cổng không cho hủy sớm");

        verify(orderRepository, never()).save(any());
    }

    private PaymentRecord givenPendingAttempt(Instant createdAt, PaymentLinkRemoteStatus remoteStatus) {
        var attempt = PaymentRecord.forEBankingCheckout(
            ORDER_ID, new BigDecimal("500000"), PaymentProvider.PAYOS, "orphan-ref", createdAt);
        attempt.setId(UUID.randomUUID());
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(attempt));
        when(paymentRecordRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(remoteStatus));
        return attempt;
    }

    private static Order pendingOrder() {
        var order = new Order();
        order.setId(ORDER_ID);
        order.setSchoolId(SCHOOL_ID);
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmountVnd(new BigDecimal("500000"));
        order.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        return order;
    }
}
