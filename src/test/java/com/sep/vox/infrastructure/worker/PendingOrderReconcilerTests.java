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
 * Phép kiểm cho ca DÒNG MỒ CÔI: lần thử mang một mã mà cổng chưa từng thấy.
 *
 * <p>Trạng thái này chỉ tồn tại từ khi CreatePaymentCheckoutUrlUseCase commit dòng PENDING TRƯỚC lúc
 * gọi createPaymentLink -- lời gọi đó hỏng giữa chừng thì dòng ở lại, còn cổng thì không có phiên nào.
 *
 * <p>Vì sao đáng có test riêng: job này KHÔNG chốt dòng mồ côi thì {@code expireIfOverdue} từ chối
 * đóng đơn (nó thấy còn lần thử treo), nên đơn kẹt PENDING VĨNH VIỄN chứ không phải tới lúc hết hạn
 * -- hết hạn chính là việc đang bị chặn. Với đơn đăng ký thì
 * uq_orders_one_open_subscription_order khóa luôn trường khỏi việc đặt đơn mới, và đường thoát thủ
 * công (CancelOrderUseCase) cũng đi qua đúng cùng một quyết định. Hỏng ở đây là hỏng thành phải sửa
 * tay dưới DB.
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
     * Mặt kia: lần thử vừa mở vài giây trước thì NOT_FOUND KHÔNG có nghĩa là nó chết -- rất có thể
     * createPaymentLink còn đang bay và cổng chưa kịp dựng phiên. Job nền là thứ dễ rơi vào đúng cửa
     * sổ đó nhất, và chốt hỏng ở đây nghĩa là trường trả tiền vào một link thật trong khi dòng ứng
     * với khoản tiền ấy đã mang FAILED.
     */
    @Test
    void should_leave_a_freshly_opened_attempt_alone_when_the_gateway_says_not_found() {
        givenPendingAttempt(Instant.now().minus(3, ChronoUnit.SECONDS), PaymentLinkRemoteStatus.NOT_FOUND);

        reconciler.reconcile();

        verify(orderSettlementService, never()).failAttempt(any(), any());
    }

    /** Cổng nói phiên vẫn đang chờ trả: không đụng vào gì cả, để lượt sau hỏi lại. */
    @Test
    void should_leave_a_live_attempt_alone() {
        givenPendingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES), PaymentLinkRemoteStatus.PENDING);

        reconciler.reconcile();

        verify(orderSettlementService, never()).failAttempt(any(), any());
        verify(orderSettlementService, never()).settlePaid(any(), any());
    }

    private void givenPendingAttempt(Instant createdAt, PaymentLinkRemoteStatus remoteStatus) {
        var attempt = PaymentRecord.forEBankingCheckout(
            ORDER_ID, new BigDecimal("500000"), PaymentProvider.PAYOS, "orphan-ref", createdAt);
        attempt.setId(UUID.randomUUID());
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(attempt));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(remoteStatus));
    }

    private static Order overdueOrder() {
        var order = new Order();
        order.setId(ORDER_ID);
        order.setSchoolId(UUID.randomUUID());
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmountVnd(new BigDecimal("500000"));
        order.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return order;
    }
}
