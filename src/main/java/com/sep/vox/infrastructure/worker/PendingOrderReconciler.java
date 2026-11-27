package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Lưới an toàn cho các đơn còn PENDING: đi hỏi thẳng cổng thanh toán thay vì ngồi đợi callback.
 *
 * <p>Cần thiết vì callback KHÔNG được đảm bảo: trường đóng tab giữa chừng, webhook trượt lúc mình
 * đang deploy, hoặc cổng đơn giản là gọi hụt. Không có job này thì tiền đã vào tài khoản mà đơn vẫn
 * treo, trường không nhận được hàng và cũng không đặt lại được (uq_orders_one_open_subscription_order
 * chặn đơn đăng ký thứ hai).
 *
 * <p>Thay cho PendingInvoiceReconciler cũ. Khác biệt về bản chất: bản cũ quét HÓA ĐƠN vì hóa đơn
 * mang luôn trạng thái thanh toán; giờ hóa đơn chỉ phát sau khi tiền về, nên thứ phải quét là ĐƠN,
 * còn thứ đem đi hỏi cổng là LẦN THỬ gắn với đơn đó.
 *
 * <p>Hai việc trong CÙNG một lượt và đúng thứ tự này -- đối soát trước, hết hạn sau -- vì chúng phụ
 * thuộc nhau: một đơn vừa quá hạn vừa còn lần thử treo thì rất có thể trường vừa trả ở phút chót.
 * Đóng đơn trước khi hỏi cổng là tự tạo ra ca "đã thu tiền nhưng không còn đơn để giao".
 *
 * <p>Cả hai cổng đều tự gửi lại khi không nhận được phản hồi thành công, nhưng KHÔNG cổng nào công
 * bố lịch retry cho sản phẩm đang dùng (SePay chỉ công bố lịch 8 lần/33 phút cho webhook biến động
 * số dư -- một sản phẩm KHÁC với Payment Gateway ở đây). Nghĩa là không được coi retry của cổng là
 * lưới an toàn: job này mới là đường phục hồi duy nhất mình kiểm soát được.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderReconciler {

    // 400ms between two polls (~2.5 req/s). SePay limits a few requests per second and answers 429 with the header
    // X-SePay-UserApi-Retry-After (see SePayPaymentProcessService.getPaymentLinkStatus); PayOS does not
    // publish a specific threshold, so this value follows the stricter side. The job runs in the background: a few
    // extra seconds per sweep go unnoticed, while being blocked by the gateway for flooding it would lose the
    // reconciliation path altogether.
    private static final long THROTTLE_MILLIS = 400L;

    // Upper bound on gateway calls per sweep so an abnormal backlog does not turn the job into API calls that
    // run for tens of minutes. The rest is left for the next sweep (5 minutes later).
    private static final int MAX_REMOTE_CALLS_PER_RUN = 200;

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final OrderSettlementService orderSettlementService;

    /**
     * NOT {@code @Transactional} here. A sweep can take several minutes because it has to call out to the external network, and
     * wrapping everything in one transaction would hold a DB connection for that whole time and one failing order
     * would drag down every order already settled successfully before it. Each order settles itself in its own transaction in
     * OrderSettlementService.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        Instant now = Instant.now();
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return;
        }

        SweepProgress progress = new SweepProgress();
        for (Order order : pendingOrders) {
            if (!reconcileOrder(order, now, progress)) {
                break;
            }
        }

        if (progress.settled > 0 || progress.expired > 0) {
            log.info("[RECONCILER] Quét {} đơn PENDING: chốt {} đơn đã thu được tiền, đóng {} đơn quá hạn",
                pendingOrders.size(), progress.settled, progress.expired);
        }
    }

    private static final class SweepProgress {
        private int remoteCalls;
        private int settled;
        private int expired;
    }

    /**
     * @return false when the sweep must stop (call budget used up, or interrupted while pacing)
     */
    private boolean reconcileOrder(Order order, Instant now, SweepProgress progress) {
        PaymentRecord pendingAttempt = paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);

        if (pendingAttempt != null) {
            if (!reserveRemoteCall(progress)) {
                return false;
            }
            if (reconcileAttempt(order, pendingAttempt)) {
                progress.settled++;
                // An order just settled as PAID is no longer overdue -- move on to the next order.
                return true;
            }
        }

        // Try to close the order if it is overdue. Do NOT assume here that the order has no pending attempt left:
        // reconcileAttempt returns false both for "the gateway says it is still waiting for payment" and for a failed gateway call.
        // expireIfOverdue re-checks under a row lock and refuses by itself while a PENDING row remains.
        if (expireOverdue(order, now)) {
            progress.expired++;
        }
        return true;
    }

    private boolean reserveRemoteCall(SweepProgress progress) {
        if (progress.remoteCalls >= MAX_REMOTE_CALLS_PER_RUN) {
            log.warn("Dừng đối soát ở {} lần gọi cổng trong lượt này, phần còn lại để lượt sau",
                MAX_REMOTE_CALLS_PER_RUN);
            return false;
        }
        // Pace BEFORE each call (except the first) instead of after: if the loop exits early it also
        // does not pay for a useless sleep.
        if (progress.remoteCalls > 0 && !throttle()) {
            return false;
        }
        progress.remoteCalls++;
        return true;
    }

    /**
     * Ask the gateway about a pending attempt and settle it.
     *
     * @return true if the money has arrived and the order was just fulfilled
     */
    private boolean reconcileAttempt(Order order, PaymentRecord attempt) {
        try {
            PaymentLinkRemoteStatus remoteStatus = paymentProcessResolver.resolve(attempt.getProvider())
                .getPaymentLinkStatus(attempt.getProviderOrderRef())
                .status();

            if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                settleAsPaid(attempt);
                return true;
            }
            failAttemptIfFinal(attempt, remoteStatus);
            return false;
        } catch (Exception e) {
            // Swallow the error of EACH order: a failing gateway must not be allowed to block reconciling the remaining
            // orders in the same sweep.
            log.warn("Lỗi khi đối soát đơn {} provider={} orderRef={}",
                order.getId(), attempt.getProvider(), attempt.getProviderOrderRef(), e);
            return false;
        }
    }

    private void settleAsPaid(PaymentRecord attempt) {
        log.info("[RECONCILER] Chốt lần thanh toán {} (orderRef={}) thành PAID qua polling {}",
            attempt.getId(), attempt.getProviderOrderRef(), attempt.getProvider());
        orderSettlementService.settlePaid(attempt, Instant.now());
    }

    // Close only the ATTEMPT, not the order -- same reason as in the callback: the school must be able to press pay
    // again without placing a new order. See OrderSettlementService.failAttempt.
    private void failAttemptIfFinal(PaymentRecord attempt, PaymentLinkRemoteStatus remoteStatus) {
        if (isFinalFailure(remoteStatus, attempt)) {
            log.info("[RECONCILER] Chốt lần thanh toán {} (orderRef={}) thành FAILED do cổng {} báo {}",
                attempt.getId(), attempt.getProviderOrderRef(), attempt.getProvider(), remoteStatus);
            orderSettlementService.failAttempt(attempt, PaymentStatus.FAILED);
        }
    }

    private boolean expireOverdue(Order order, Instant now) {
        try {
            return orderSettlementService.expireIfOverdue(order.getId(), now);
        } catch (Exception e) {
            log.warn("Lỗi khi đóng đơn quá hạn {}", order.getId(), e);
            return false;
        }
    }

    /**
     * null hoặc PENDING/PROCESSING/UNDERPAID = chưa phải trạng thái cuối, chưa chốt gì.
     *
     * <p>NOT_FOUND PHẢI nằm ở đây, và đây là chỗ quan trọng nhất trong ba chỗ xử lý nó. Dòng mồ côi
     * (cổng chưa từng thấy mã, sinh ra khi createPaymentLink hỏng giữa chừng) mà không được chốt thì
     * {@code expireIfOverdue} TỪ CHỐI đóng đơn -- nó thấy còn lần thử treo nên trả false. Đơn kẹt
     * PENDING vĩnh viễn chứ không phải tới lúc hết hạn: hết hạn là thứ chính job này phải thực hiện,
     * mà nó lại đang bị chặn bởi đúng dòng nó không chịu dọn. Với đơn đăng ký thì
     * uq_orders_one_open_subscription_order khóa luôn trường khỏi việc đặt đơn mới -- cũng vĩnh viễn.
     */
    private boolean isFinalFailure(PaymentLinkRemoteStatus status, PaymentRecord attempt) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case CANCELLED, EXPIRED, FAILED -> true;
            // Ân hạn trước khi tin NOT_FOUND -- xem PaymentRecord.canRetireOnGatewayNotFound. Job
            // này chạy nền nên nó chính là thứ dễ rơi vào giữa lúc createPaymentLink còn đang bay.
            case NOT_FOUND -> attempt.canRetireOnGatewayNotFound(Instant.now());
            default -> false;
        };
    }

    // Trả false khi bị ngắt (vd ứng dụng đang tắt) để vòng lặp dừng hẳn thay vì chạy tiếp không
    // throttle và dội API của cổng.
    private boolean throttle() {
        try {
            Thread.sleep(THROTTLE_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Job đối soát bị ngắt giữa chừng, dừng lượt quét này");
            return false;
        }
    }
}
