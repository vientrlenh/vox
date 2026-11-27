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

    private static final String CANCELLATION_REASON = "The school cancelled the order itself";

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
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("The order is no longer awaiting payment, so it cannot be cancelled.");
        }

        closeLivePaymentAttempt(order);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("School {} cancelled order {}", order.getSchoolId(), order.getId());
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
                "This order has already been paid and the system is recording it. It cannot be cancelled.");
        }

        // An attempt that was just opened but the gateway says does not exist: most likely another tab's
        // createPaymentLink is still in flight. Make them wait out the grace period and cancel again -- see
        // PaymentRecord.canRetireOnGatewayNotFound. Kept separate from the branch below so the message says what
        // to do: this case only needs a retry in a few minutes, not "wait for the order to expire".
        if (remoteStatus == PaymentLinkRemoteStatus.NOT_FOUND
                && !attempt.canRetireOnGatewayNotFound(Instant.now())) {
            throw new IllegalArgumentException(
                "The order has just opened a payment session that the gateway has not confirmed yet. Please try again in a few minutes.");
        }
    }

    // The session is still alive -> it must be closable on the gateway side before the order is dared to be closed.
    private void requireGatewayCancelled(PaymentProcessPort paymentPort, PaymentRecord attempt) {
        if (!paymentPort.cancelPaymentLink(attempt.getProviderOrderRef(), CANCELLATION_REASON)) {
            // SePay lands here EVERY time because the gateway has no API to cancel an unpaid session -- see
            // PaymentProcessPort.cancelPaymentLink. We accept making the school wait for expiry (at most 24
            // hours, and that deadline is already shown on the order) instead of opening a payment for a closed order.
            throw new IllegalArgumentException(
                "The order has a pending payment session that the gateway does not let you cancel early. "
                    + "Please complete that session or wait for the order to expire.");
        }
    }

    /**
     * Closes the payment record of the order being cancelled.
     *
     * <p>WHAT KEEPS THIS SAFE is the order lock in {@code execute}, NOT the re-read below.
     * {@code OrderSettlementService.settlePaid} -- the ONLY path that moves an attempt to PAID -- starts with
     * {@code findByIdForUpdate} on this very order, so as long as we hold the lock it waits and cannot settle in
     * between the two network calls above.
     *
     * <p>The re-read here does NOT protect anything by itself: {@code findPendingByOrderId} above already loaded
     * this row into the persistence context, so {@code findById} returns the in-memory copy without touching the
     * DB -- the two guards below therefore always see the state of the first read. They are kept because they are
     * cheap and because they state the intent, but do NOT rely on them: whoever adds a path that writes
     * payment_records without going through the order lock must lock again here, not count on this read.
     *
     * <p>({@code failAttempt} does write payment_records without holding the order lock, but it only moves to the
     * failed status -- the same thing we are about to write, so it is harmless.)
     */
    private void markAttemptFailed(Order order, UUID attemptId) {
        PaymentRecord current = paymentRecordRepository.findById(attemptId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment attempt of the order not found"));

        if (current.getStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException(
                "This order has just been paid and the system is recording it. It cannot be cancelled.");
        }
        if (current.isSettled()) {
            return;
        }

        current.setStatus(PaymentStatus.FAILED);
        paymentRecordRepository.save(current);
        log.info("Closing payment attempt {} of order {} because the school cancelled the order", attemptId, order.getId());
    }

    /**
     * The gateway has settled that this session will not bring in money -- no further cancellation is needed.
     *
     * <p>NOT_FOUND must be handled here (after the grace period, which the caller checks beforehand): without it an
     * orphan row falls into the "session still alive" branch below, and asking the gateway to cancel a code it does
     * not know makes cancelPaymentLink return false (PayOS swallows the error and returns false, SePay always
     * returns false) -- the school then gets the "wait for the order to expire" message while that very orphan row
     * is blocking expireIfOverdue, so the order never expires. And when the manual escape route is broken, the only
     * fix left is editing the DB by hand.
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
