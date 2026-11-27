package com.sep.vox.application.port.input.usecase.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.payment.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.payment.PaymentCheckoutResponse;
import com.sep.vox.application.response.output.BankTransferDetails;
import com.sep.vox.application.response.output.CheckoutAction;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Issues the payment URL for an order. Replaces ALL THREE old use cases
 * (CreatePaymentLinkForSubscriptionRequest / ForRenewal / ForTokenPurchase): those three flows differed in
 * "what is being bought", which now lives entirely in the Order -- by the time money is collected they are
 * identical, just an amount and an order to attach it to.
 *
 * <p>Currently only the third gateway (E_BANKING) is accepted -- see PaymentRecord.forEBankingCheckout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentCheckoutUrlUseCase
        implements IUseCase<CreatePaymentCheckoutUrlCommand, PaymentCheckoutResponse> {

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    /**
     * There is NO {@code @Transactional} here, and that is the main point of this whole class.
     *
     * <p>The previous version wrapped the whole function in ONE transaction, so two things went wrong together:
     *
     * <ul>
     *   <li>"Write the payment_records row BEFORE calling the gateway" was only "before" within the transaction's
     *   scope. The gateway built the session but the response was lost (timeout) -> rollback, which also erased the
     *   row carrying provider_order_ref -- while the payment page on the gateway side was still alive. If the school
     *   paid into it, the callback's reverse lookup found no attempt (UNKNOWN_PAYMENT), and the reconciliation job had
     *   nothing to ask about. Money arrived, nothing delivered, and no trace left on our side.</li>
     *   <li>The FOR UPDATE lock on the order row was held across TWO network calls. A slow gateway keeps the order row
     *   stuck, and {@code OrderSettlementService.settlePaid} -- which also opens with findByIdForUpdate -- must queue
     *   behind it. A failing gateway then blocks the very path that records incoming money.</li>
     * </ul>
     *
     * <p>So it is split into SHORT transactions, each CLOSED before going out to the network. The PENDING row is
     * therefore really committed before the create-link call: if that call fails the row stays, and the next press
     * goes down the {@code resolvePendingAttempt} branch to ask the gateway whether that code brought in money.
     *
     * <p>It can NOT be fixed by wrapping only the PENDING-row write in REQUIRES_NEW while keeping the outer
     * transaction: the INSERT into payment_records has a foreign key pointing at orders, so Postgres takes
     * {@code FOR KEY SHARE} on exactly the order row that the outer transaction holds {@code FOR UPDATE}. Those two
     * modes conflict, so the inner transaction waits for the outer one while the outer one is waiting for the
     * inner -- locking itself until lock_timeout.
     */
    @Override
    public PaymentCheckoutResponse execute(CreatePaymentCheckoutUrlCommand input) {
        PaymentProvider provider = input.provider();
        requireProviderConfigured(provider);

        PaymentCheckoutResponse reusable = reusePendingAttempt(input.orderId());
        if (reusable != null) {
            return reusable;
        }

        // The PENDING row is COMMITTED here, before the second network trip.
        PreparedAttempt prepared = inTransaction(() -> openAttempt(input.orderId(), provider));
        PaymentCheckoutResult result = createPaymentLink(provider, prepared);
        storeCheckoutData(prepared, result);

        return PaymentCheckoutResponse.from(
            input.orderId(), prepared.paymentId(), prepared.providerOrderRef(), provider, result);
    }

    // Block BEFORE any transaction, and this is the only place it can be blocked.
    //
    // openAttempt COMMITS the payment_records row and only then calls out to the gateway -- deliberately, so a call that
    // fails midway still leaves a reconciliation trace. But if the gateway is missing configuration that call is certain
    // to die, and the PENDING row stays forever: cancelling the order has to ask the gateway and so fails too, issuing a
    // new link has to ask the gateway and fails too, and expireIfOverdue refuses to close an order while an attempt is
    // hanging. The order is stuck PENDING forever and the school cannot place any other subscription order.
    //
    // The existing recovery path (gateway answers NOT_FOUND -> canRetireOnGatewayNotFound) cannot save this case because
    // it needs the gateway to ANSWER. Missing configuration is the only case known for sure before calling, so it must
    // stop here rather than leave a row nobody can clean up.
    private void requireProviderConfigured(PaymentProvider provider) {
        if (!paymentProcessResolver.resolve(provider).isConfigured()) {
            throw new IllegalArgumentException(
                "Payment gateway " + provider + " is not configured on the system. Please choose another gateway or contact support.");
        }
    }

    private @Nullable PaymentCheckoutResponse reusePendingAttempt(UUID orderId) {
        PaymentRecord pending = inTransaction(() -> loadPendingAttempt(orderId));
        if (pending == null) {
            return null;
        }
        // Call the gateway OUTSIDE every transaction: this is the first network trip.
        PaymentLinkRemoteStatus remoteStatus = paymentProcessResolver.resolve(pending.getProvider())
            .getPaymentLinkStatus(pending.getProviderOrderRef())
            .status();

        return inTransaction(() -> resolvePendingAttempt(orderId, pending, remoteStatus));
    }

    private PaymentCheckoutResult createPaymentLink(PaymentProvider provider, PreparedAttempt prepared) {
        return paymentProcessResolver.resolve(provider).createPaymentLink(new CreatePaymentLinkCommand(
            prepared.providerOrderRef(),
            prepared.amountVnd(),
            "VOX-" + prepared.providerOrderRef(),
            // The link's expiry comes from the ORDER, not from an adapter constant: the link must not outlive the order
            // (paying for a dead order) and the order must not outlive the link (an order holding a slot with no way
            // left to pay).
            prepared.expiresAt()
        ));
    }

    // Store what lets this attempt be rebuilt.
    //
    // REDIRECT/QR: keep checkout_url. For FORM_POST do NOT -- its actionUrl is a FIXED endpoint shared by every order,
    // while everything that identifies the session sits in the signed field set, which is deliberately not stored.
    // Storing only that URL and redirecting straight to it next time would throw the school onto an empty init page,
    // not their session.
    //
    // QR: also store the code string and the transfer details. Asking for the status only returns the STATUS, the
    // gateway does not re-issue the code -- not stored here, there is nowhere left to get it back.
    private void storeCheckoutData(PreparedAttempt prepared, PaymentCheckoutResult result) {
        if (result.action() != CheckoutAction.FORM_POST) {
            inTransaction(() -> attachCheckoutData(
                prepared.paymentId(), result.actionUrl(), qrPayloadJson(result)));
        }
    }

    /**
     * Locks the order, checks permission and status, and returns the pending attempt (or null).
     *
     * <p>The lock is released as soon as this transaction closes, i.e. BEFORE the gateway call. It is still needed:
     * a school opening two tabs and clicking almost at the same time is normal, and the lock makes those two
     * threads see the same truth about "is there already a pending attempt" instead of both reading and then both
     * issuing a link.
     */
    private PaymentRecord loadPendingAttempt(UUID orderId) {
        Order order = requirePayableOrder(orderId);
        return paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);
    }

    /**
     * Handles the PENDING attempt FIRST, before thinking of issuing a new one, because
     * uq_payment_records_one_pending_per_order only allows one pending attempt per order.
     *
     * <p>It must GO AND ASK THE GATEWAY rather than infer from our own data -- that was already done in
     * {@code execute}, outside the transaction. A row that is still PENDING only says nobody has reported back to us
     * yet, not that the school has not paid; skipping the ask and issuing a new link opens exactly the door to
     * collecting twice that every constraint in V2 is trying to close.
     *
     * <p>Returns a response if the old attempt can be reused, returns null if it is settled and a new attempt may be
     * issued.
     */
    private @Nullable PaymentCheckoutResponse resolvePendingAttempt(
            UUID orderId, PaymentRecord pending, PaymentLinkRemoteStatus remoteStatus) {
        Order order = requirePayableOrder(orderId);

        switch (remoteStatus) {
            case PAID -> throw new IllegalArgumentException(
                "This order has already been paid and the system is recording it. Please wait a moment.");

            case CANCELLED, EXPIRED, FAILED, NOT_FOUND -> {
                failDeadAttempt(pending, remoteStatus);
                return null;
            }

            // The gateway says it is still waiting for payment: return EXACTLY the old link instead of issuing a new
            // one. Issuing a new one leaves the old link payable with no PENDING row matching it.
            default -> {
                return reuseStillPendingAttempt(order, pending);
            }
        }
    }

    private void failDeadAttempt(PaymentRecord pending, PaymentLinkRemoteStatus remoteStatus) {
        // Grace period for NOT_FOUND: the pending attempt may belong to ANOTHER TAB that has just committed the PENDING
        // row and is waiting for createPaymentLink to return. Failing it at that moment spoils an attempt about to come
        // alive -- see PaymentRecord.canRetireOnGatewayNotFound.
        if (remoteStatus == PaymentLinkRemoteStatus.NOT_FOUND
                && !pending.canRetireOnGatewayNotFound(Instant.now())) {
            throw new IllegalArgumentException(
                "The order has just opened a payment session that the gateway has not confirmed yet. "
                    + "Please try again in a few minutes.");
        }
        // The gateway has confirmed this attempt does NOT bring in money -- settle it to make room for the new one.
        // It may only be settled AFTER that confirmation: failing an attempt that is still alive means that when the
        // school pays into the old link there is no row left waiting for that money.
        //
        // NOT_FOUND is in this group because it is a direct consequence of committing the PENDING row before calling
        // the gateway: a create-link call that fails BEFORE the gateway built the session leaves a row carrying a code
        // the gateway has never seen. Without this branch that attempt would hang until the order expires and the
        // school could not get a new link. Safe because the gateway asserts that no session carries that code -- very
        // different from "could not ask", which the adapter throws out rather than mapping to here.
        pending.setStatus(PaymentStatus.FAILED);
        paymentRecordRepository.save(pending);
    }

    private PaymentCheckoutResponse reuseStillPendingAttempt(Order order, PaymentRecord pending) {
        // Gateway with QR: rebuild the code from the stored payload. This is the NORMAL path, not a rare case --
        // scanning a QR means leaving the page for the banking app and coming back.
        StoredQrPayload qr = readQrPayload(pending);
        if (qr != null) {
            return PaymentCheckoutResponse.qrFor(
                order.getId(), pending.getId(), pending.getProviderOrderRef(),
                pending.getProvider(), qr.qrCode(), qr.transfer(), pending.getCheckoutUrl());
        }

        if (pending.getCheckoutUrl() == null) {
            // FORM_POST gateway: the signed field set is deliberately not stored and cannot be rebuilt
            // here. Making the school wait for the old attempt to expire is better than issuing a
            // second parallel link.
            throw new IllegalArgumentException(
                "The order has a pending payment session. Please complete that session or try again later.");
        }
        return PaymentCheckoutResponse.redirectTo(
            order.getId(), pending.getId(), pending.getProviderOrderRef(),
            pending.getProvider(), pending.getCheckoutUrl());
    }

    /**
     * Settles a new attempt and COMMITS it, so that the gateway call that follows always has a row representing it.
     *
     * <p>Locks the order again and re-checks everything instead of trusting the result of
     * {@code loadPendingAttempt}: between the two transactions the order may have been cancelled, may have
     * expired, or another tab may already have opened its own attempt.
     */
    private PreparedAttempt openAttempt(UUID orderId, PaymentProvider provider) {
        Order order = requirePayableOrder(orderId);

        // Never send an amount <= 0 to the gateway. No gateway accepts it, so if it got through the school would receive
        // an unreadable provider error while the order sits PENDING until it expires. The path that produced 0 VND
        // orders was closed in SubscriptionUpgradePolicyService.calculateUnusedCredit; this guard is here so that
        // every FUTURE path leading to the same place stops with a message the reader understands, not to fix
        // the same bug again.
        if (order.getTotalAmountVnd() == null || order.getTotalAmountVnd().signum() <= 0) {
            throw new IllegalArgumentException(
                "The order has no amount to pay, so a payment session cannot be created. Please contact support.");
        }

        // The order code is generated by the adapter and is ALWAYS NEW, even when this is a repayment of the same order:
        // PayOS rejects a duplicate orderCode and SePay requires a unique order_invoice_number. Both adapters generate
        // the code locally (clock / UUID) without a network call, so calling it inside the transaction is safe.
        String providerOrderRef = paymentProcessResolver.resolve(provider).newOrderRef();

        PaymentRecord payment = paymentRecordRepository.save(PaymentRecord.forEBankingCheckout(
            order.getId(), order.getTotalAmountVnd(), provider, providerOrderRef, Instant.now()));

        return new PreparedAttempt(
            payment.getId(), providerOrderRef, order.getTotalAmountVnd(), order.getExpiresAt());
    }

    /**
     * Stores only the URL. The FORM_POST field set carries an HMAC signature -- storing it in the DB would let
     * anyone who can read the DB rebuild a valid checkout, and since it can be recomputed anyway, storing it adds
     * nothing.
     *
     * <p>Does not lock the order here: it only touches the payment_records row that was just created, and whether
     * that row may exist was already settled in {@code openAttempt}.
     */
    private Void attachCheckoutData(UUID paymentId, String checkoutUrl, String qrPayloadJson) {
        PaymentRecord payment = paymentRecordRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("The payment attempt that was just created was not found"));
        payment.setCheckoutUrl(checkoutUrl);
        if (qrPayloadJson != null) {
            payment.setProviderPayloadJson(qrPayloadJson);
        }
        paymentRecordRepository.save(payment);
        return null;
    }

    /** The QR string + bank transfer details, as JSON to store in provider_payload_json. */
    private String qrPayloadJson(PaymentCheckoutResult result) {
        if (result.action() != CheckoutAction.QR) {
            return null;
        }
        try {
            return jsonSerializationPort.toJson(new StoredQrPayload(result.qrCode(), result.transfer()));
        } catch (RuntimeException e) {
            // Do not break the whole payment just because the payload could not be stored: the code can still be shown this
            // time, only the ability to rebuild it when the school returns to the order is lost. The checkout_url branch in
            // resolvePendingAttempt still sends them to the gateway page then.
            log.warn("Could not store the QR payload for payment attempt {} -- next time the gateway page will have to be opened",
                result.paymentLinkId(), e);
            return null;
        }
    }

    /** Reads back the stored payload; returns null if this attempt is not of the QR kind or the payload is corrupt. */
    private StoredQrPayload readQrPayload(PaymentRecord pending) {
        String json = pending.getProviderPayloadJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            StoredQrPayload payload = jsonSerializationPort.fromJson(json, StoredQrPayload.class);
            return payload.qrCode() == null ? null : payload;
        } catch (RuntimeException e) {
            log.warn("The QR payload of payment attempt {} cannot be read -- ignoring it and using the checkout_url path",
                pending.getId(), e);
            return null;
        }
    }

    /** The shape of provider_payload_json. It does NOT contain the signature -- see PaymentRecordJpaEntity. */
    private record StoredQrPayload(String qrCode, BankTransferDetails transfer) {}

    /**
     * Locks the order row, then checks the three conditions required to issue a link. Called at the START of every
     * transaction that writes, because each transaction is a fresh look at the data.
     */
    private Order requirePayableOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("The order is no longer awaiting payment.");
        }
        // An overdue order does NOT get a new link: this deadline was already sent to the gateway with the earlier attempt,
        // and granting more time here would mean changing a commitment that is already made. To buy again, place a new order.
        if (!Instant.now().isBefore(order.getExpiresAt())) {
            throw new IllegalArgumentException("The order's payment period has expired; please place a new order.");
        }
        return order;
    }

    /**
     * REQUIRES_NEW and not REQUIRED: if someone ever calls this use case from inside an existing
     * transaction, REQUIRED would join that transaction and the whole class would fall back into the
     * original bug -- the order lock held across the network call, and the PENDING row rolled back
     * with it. This flag makes every phase close itself regardless of the calling context.
     *
     * <p>It is safe because the current caller is RestPaymentController (REST, no transaction), so no
     * order row is locked outside that could end up waiting on each other.
     */
    private <T> T inTransaction(Supplier<T> work) {
        return requiresNewTransactionPort.callInNewTransaction(work);
    }

    /** Data that has to cross the transaction boundary for the gateway call -- all POJOs already detached from the persistence context. */
    private record PreparedAttempt(
        UUID paymentId, String providerOrderRef, BigDecimal amountVnd, Instant expiresAt) {}
}
