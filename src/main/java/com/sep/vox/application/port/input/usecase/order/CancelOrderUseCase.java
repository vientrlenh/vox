package com.sep.vox.application.port.input.usecase.order;

import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.domain.model.payment.PaymentRecord;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.order.CancelOrderCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * The school abandons an order it has not paid yet.
 *
 * <p>It exists because {@code uq_orders_one_open_subscription_order} only allows ONE open subscription order per school:
 * without a cancel path, a school that ordered the wrong plan is blocked from placing a new order until the old one
 * expires by itself (at most 24 hours per Order.PENDING_TTL). This is the MANUAL version of what PendingOrderReconciler
 * does automatically when an order is overdue.
 *
 * <p>The hard part is NOT changing the order status but the payment session that is still alive on the gateway side:
 * closing the order while leaving the link alive means the school can still pay into it, and when the money arrives
 * OrderSettlementService sees the order is no longer PENDING and delivers nothing -- the money is collected and nothing is
 * granted, and the system has no refund flow yet. So the rule here is: only close the order when it is CERTAIN that no
 * path for money remains.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancelOrderUseCase implements IUseCase<CancelOrderCommand, UUID> {

    private static final String CANCELLATION_REASON = "Trường chủ động hủy đơn";

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CancelOrderCommand input) {
        // Lock the order: a cancellation and a "paid" callback may well arrive at the same time. Without the lock both see the
        // order as PENDING, one closes it CANCELLED and the other closes it SUCCESS -- the write order decides the outcome, and one
        // of the two outcomes is wrong.
        Order order = orderRepository.findByIdForUpdate(input.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Đơn hàng không còn ở trạng thái chờ thanh toán, không thể hủy.");
        }

        closeLivePaymentAttempt(order);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Trường {} hủy đơn {}", order.getSchoolId(), order.getId());
        return order.getId();
    }

    /**
     * Makes sure the order has no session left that could bring in money, or throws. Returning normally = safe to
     * close the order.
     */
    private void closeLivePaymentAttempt(Order order) {
        PaymentRecord attempt = paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);
        if (attempt == null) {
            return;
        }
        PaymentProcessPort paymentPort = paymentProcessResolver.resolve(attempt.getProvider());
        // Ask the gateway BEFORE doing anything else: a row that is still PENDING only says nobody has reported back to
        // us yet, not that the school has not paid.
        PaymentLinkRemoteStatus remoteStatus = paymentPort.getPaymentLinkStatus(attempt.getProviderOrderRef()).status();
        requireNotPaidAndNotJustOpened(remoteStatus, attempt);

        // The gateway has confirmed the session is already dead (the school pressed cancel on the gateway page, or the
        // link expired on its own): no need to call cancel, just settle our own row to match.
        if (isAlreadyDead(remoteStatus)) {
            markAttemptFailed(order, attempt.getId());
            return;
        }
        requireGatewayCancelled(paymentPort, attempt);
        markAttemptFailed(order, attempt.getId());
    }

    private void requireNotPaidAndNotJustOpened(PaymentLinkRemoteStatus remoteStatus, PaymentRecord attempt) {
        if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
            throw new IllegalArgumentException(
                "Đơn hàng này đã được thanh toán, hệ thống đang ghi nhận. Không thể hủy.");
        }

        // An attempt that was just opened but the gateway says does not exist: most likely another tab's
        // createPaymentLink is still in flight. Make them wait out the grace period and cancel again -- see
        // PaymentRecord.canRetireOnGatewayNotFound. Kept separate from the branch below so the message says what
        // to do: this case only needs a retry in a few minutes, not "wait for the order to expire".
        if (remoteStatus == PaymentLinkRemoteStatus.NOT_FOUND
                && !attempt.canRetireOnGatewayNotFound(Instant.now())) {
            throw new IllegalArgumentException(
                "Đơn hàng vừa mở một phiên thanh toán mà cổng chưa xác nhận. Vui lòng thử lại sau vài phút.");
        }
    }

    // The session is still alive -> it must be closable on the gateway side before the order is dared to be closed.
    private void requireGatewayCancelled(PaymentProcessPort paymentPort, PaymentRecord attempt) {
        if (!paymentPort.cancelPaymentLink(attempt.getProviderOrderRef(), CANCELLATION_REASON)) {
            // SePay lands here EVERY time because the gateway has no API to cancel an unpaid session -- see
            // PaymentProcessPort.cancelPaymentLink. We accept making the school wait for expiry (at most 24
            // hours, and that deadline is already shown on the order) instead of opening a payment for a closed order.
            throw new IllegalArgumentException(
                "Đơn hàng đang có một phiên thanh toán chờ xử lý mà cổng không cho hủy sớm. "
                    + "Vui lòng hoàn tất phiên đó hoặc đợi đơn hết hạn.");
        }
    }

    /**
     * Đóng dòng thanh toán của đơn đang bị hủy.
     *
     * <p>ĐIỀU GIỮ CHO CHỖ NÀY AN TOÀN là khóa đơn ở {@code execute}, KHÔNG phải lần đọc lại dưới đây.
     * {@code OrderSettlementService.settlePaid} -- đường DUY NHẤT đẩy một lần thử sang PAID -- mở đầu
     * bằng {@code findByIdForUpdate} trên chính đơn này, nên suốt lúc ta còn giữ khóa thì nó nằm chờ,
     * không thể chốt xen vào giữa hai lần gọi mạng ở trên.
     *
     * <p>Lần đọc lại ở đây KHÔNG tự nó chống được gì: {@code findPendingByOrderId} phía trên đã nạp
     * đúng dòng này vào persistence context, nên {@code findById} lấy lại bản trong bộ nhớ chứ không
     * chạm DB -- hai chốt bên dưới vì thế luôn nhìn thấy trạng thái của lần đọc đầu. Giữ lại vì chúng
     * rẻ và vì chúng nói đúng ý định, nhưng ĐỪNG dựa vào chúng: ai thêm một đường ghi payment_records
     * mà không đi qua khóa đơn thì phải khóa lại ở đây, không phải trông vào lần đọc này.
     *
     * <p>({@code failAttempt} có ghi payment_records mà không giữ khóa đơn, nhưng nó chỉ chuyển sang
     * trạng thái thất bại -- trùng đúng thứ ta sắp ghi, nên vô hại.)
     */
    private void markAttemptFailed(Order order, UUID attemptId) {
        PaymentRecord current = paymentRecordRepository.findById(attemptId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lần thanh toán của đơn"));

        if (current.getStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException(
                "Đơn hàng này vừa được thanh toán, hệ thống đang ghi nhận. Không thể hủy.");
        }
        if (current.isSettled()) {
            return;
        }

        current.setStatus(PaymentStatus.FAILED);
        paymentRecordRepository.save(current);
        log.info("Đóng lần thanh toán {} của đơn {} do trường hủy đơn", attemptId, order.getId());
    }

    /**
     * Cổng đã chốt là phiên này không ra tiền -- không cần hủy thêm.
     *
     * <p>NOT_FOUND phải nằm ở đây (đã qua ân hạn, chỗ gọi kiểm trước): thiếu nó thì một dòng mồ côi
     * rơi xuống nhánh "phiên vẫn sống" bên dưới, mà gọi hủy một mã cổng không biết thì
     * cancelPaymentLink trả false (PayOS nuốt lỗi rồi trả false, SePay vốn luôn trả false) -- trường
     * nhận câu "đợi đơn hết hạn" trong khi chính dòng mồ côi ấy đang chặn expireIfOverdue, nên đơn
     * sẽ không bao giờ hết hạn. Đường thoát thủ công mà hỏng thì chỉ còn sửa tay dưới DB.
     */
    private boolean isAlreadyDead(PaymentLinkRemoteStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case CANCELLED, EXPIRED, FAILED, NOT_FOUND -> true;
            default -> false;
        };
    }
}
