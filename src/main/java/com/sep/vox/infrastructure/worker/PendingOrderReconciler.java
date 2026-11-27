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
 * Safety net for orders still PENDING: asks the payment gateway directly instead of waiting for the callback.
 *
 * <p>Needed because the callback is NOT guaranteed: the school closes the tab midway, the webhook misses while we
 * are deploying, or the gateway simply fails to call. Without this job the money is already in the account but the order stays
 * hanging, the school gets no goods and cannot order again (uq_orders_one_open_subscription_order
 * blocks a second subscription order).
 *
 * <p>Replaces the old PendingInvoiceReconciler. The essential difference: the old one scanned INVOICES because an invoice
 * carried the payment status; now an invoice is only issued after the money arrives, so what must be scanned is the ORDER,
 * and what is sent to ask the gateway is the ATTEMPT attached to that order.
 *
 * <p>Two jobs in the SAME run and in this order -- reconcile first, expire after -- because they depend
 * on each other: an order that is both overdue and still has a pending attempt very likely means the school just paid at the last minute.
 * Closing the order before asking the gateway creates the case "money collected but no order left to deliver".
 *
 * <p>Both gateways resend by themselves when they do not receive a successful response, but NEITHER gateway publishes
 * a retry schedule for the product in use (SePay only publishes a schedule of 8 attempts/33 minutes for balance-change
 * webhooks -- a DIFFERENT product from the Payment Gateway here). That means the gateway's retry must not be treated as a
 * safety net: this job is the only recovery path we control.
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
            log.info("[RECONCILER] Scanned {} PENDING orders: settled {} orders whose money was collected, closed {} overdue orders",
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
            log.warn("Stopped reconciliation at {} gateway calls in this run, the rest is left for the next run",
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
            log.warn("Error while reconciling order {} provider={} orderRef={}",
                order.getId(), attempt.getProvider(), attempt.getProviderOrderRef(), e);
            return false;
        }
    }

    private void settleAsPaid(PaymentRecord attempt) {
        log.info("[RECONCILER] Settled payment attempt {} (orderRef={}) as PAID via polling {}",
            attempt.getId(), attempt.getProviderOrderRef(), attempt.getProvider());
        orderSettlementService.settlePaid(attempt, Instant.now());
    }

    // Close only the ATTEMPT, not the order -- same reason as in the callback: the school must be able to press pay
    // again without placing a new order. See OrderSettlementService.failAttempt.
    private void failAttemptIfFinal(PaymentRecord attempt, PaymentLinkRemoteStatus remoteStatus) {
        if (isFinalFailure(remoteStatus, attempt)) {
            log.info("[RECONCILER] Settled payment attempt {} (orderRef={}) as FAILED because gateway {} reported {}",
                attempt.getId(), attempt.getProviderOrderRef(), attempt.getProvider(), remoteStatus);
            orderSettlementService.failAttempt(attempt, PaymentStatus.FAILED);
        }
    }

    private boolean expireOverdue(Order order, Instant now) {
        try {
            return orderSettlementService.expireIfOverdue(order.getId(), now);
        } catch (Exception e) {
            log.warn("Error while closing overdue order {}", order.getId(), e);
            return false;
        }
    }

    /**
     * null or PENDING/PROCESSING/UNDERPAID = not a final state yet, nothing settled.
     *
     * <p>NOT_FOUND MUST be here, and this is the most important of the three places that handle it. An orphaned row
     * (the gateway never saw the code, created when createPaymentLink failed midway) that is not settled makes
     * {@code expireIfOverdue} REFUSE to close the order -- it sees a pending attempt and returns false. The order is stuck
     * PENDING forever and not just until expiry: expiring is exactly what this job must do,
     * and it is being blocked by the very row it refuses to clean up. For a subscription order,
     * uq_orders_one_open_subscription_order also locks the school out of placing a new order -- also forever.
     */
    private boolean isFinalFailure(PaymentLinkRemoteStatus status, PaymentRecord attempt) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case CANCELLED, EXPIRED, FAILED -> true;
            // Grace period before trusting NOT_FOUND -- see PaymentRecord.canRetireOnGatewayNotFound. This job
            // runs in the background so it is exactly what is likely to land in the middle of a createPaymentLink still in flight.
            case NOT_FOUND -> attempt.canRetireOnGatewayNotFound(Instant.now());
            default -> false;
        };
    }

    // Returns false when interrupted (e.g. the application is shutting down) so the loop stops completely instead of running on without
    // throttling and hammering the gateway API.
    private boolean throttle() {
        try {
            Thread.sleep(THROTTLE_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Reconciliation job interrupted midway, stopping this sweep");
            return false;
        }
    }
}
