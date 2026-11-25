package com.sep.vox.application.port.input.usecase.payment;

import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.domain.model.payment.PaymentRecord;
import java.time.Instant;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.payment.ProcessPaymentCallbackCommand;
import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.payment.PaymentCallbackResponse;
import com.sep.vox.application.response.input.payment.PaymentCallbackResponse.CallbackOutcome;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Handles the authenticated callback of EVERY payment gateway, and settles the result for the
 * matching attempt.
 *
 * <p>The lookup key is now {@code payment_records.provider_order_ref}, not the invoice: the invoice
 * only exists AFTER the money arrives, so it does not exist yet when the callback comes in.
 * provider_order_ref is the only thing the gateway sends back that we wrote down BEFORE calling the
 * gateway -- see CreatePaymentCheckoutUrlUseCase.
 *
 * <p>The gateway uses the HTTP status code to decide whether to resend, so how the branches here
 * differ decides the gateway's retry behavior. See RestCallbackController.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentCallbackUseCase implements IUseCase<ProcessPaymentCallbackCommand, PaymentCallbackResponse> {

    private final PaymentProcessResolver paymentProcessResolver;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OrderSettlementService orderSettlementService;

    @Transactional
    public PaymentCallbackResponse execute(ProcessPaymentCallbackCommand input) {
        CallbackVerificationResult verification = verifiedCallback(input);

        Optional<PaymentRecord> paymentOpt = paymentRecordRepository.findByProviderAndProviderOrderRef(
            input.provider(), verification.providerOrderRef());
        if (paymentOpt.isEmpty()) {
            return respondToUnknownPayment(input, verification);
        }
        PaymentRecord payment = paymentOpt.get();

        if (payment.isSettled()) {
            return respondToAlreadySettled(input, verification, payment);
        }
        if (isAmountMismatch(verification, payment)) {
            return respondToAmountMismatch(input, verification, payment);
        }
        return settleOrFailAttempt(input, verification, payment);
    }

    private CallbackVerificationResult verifiedCallback(ProcessPaymentCallbackCommand input) {
        CallbackVerificationResult verification = paymentProcessResolver
                .resolve(input.provider())
                .verifyCallback(input.rawBody(), input.headers());
        if (!verification.valid()) {
            throw new UnauthorizedException("Callback " + input.provider() + " không hợp lệ");
        }
        return verification;
    }

    private PaymentCallbackResponse respondToUnknownPayment(
            ProcessPaymentCallbackCommand input, CallbackVerificationResult verification) {
        log.warn("Nhận callback {} hợp lệ nhưng không tìm thấy lần thanh toán cho orderRef={}",
            input.provider(), verification.providerOrderRef());
        return PaymentCallbackResponse.toResponse(CallbackOutcome.UNKNOWN_PAYMENT);
    }

    private PaymentCallbackResponse respondToAlreadySettled(
            ProcessPaymentCallbackCommand input, CallbackVerificationResult verification, PaymentRecord payment) {
        // A repeated callback is NORMAL, not an error: the gateway resends when it has not received a 200. Exit early
        // here instead of letting settlePaid block it itself, so that "just settled now" and "settled by an earlier
        // call" can be told apart in the reconciliation log.
        if (verification.status() == PaymentLinkRemoteStatus.PAID
                && payment.getStatus() != PaymentStatus.PAID) {
            return respondToPaidAfterWriteOff(input, verification, payment);
        }

        log.info("Callback {} lặp cho orderRef={} -- lần thử đã ở trạng thái {}",
            input.provider(), verification.providerOrderRef(), payment.getStatus());
        return PaymentCallbackResponse.toResponse(CallbackOutcome.ALREADY_SETTLED);
    }

    private PaymentCallbackResponse respondToPaidAfterWriteOff(
            ProcessPaymentCallbackCommand input, CallbackVerificationResult verification, PaymentRecord payment) {
        // "Already settled" does NOT mean "harmless". isSettled() is only status != PENDING, so an
        // attempt that was already written as FAILED also lands here -- and the gateway reporting
        // PAID for that very attempt means REAL MONEY ARRIVED for an attempt we wrote off.
        //
        // The most common way in is a webhook arriving OUT OF ORDER: the gateway fires EXPIRED
        // first, we write FAILED, and then PAID arrives. RestCallbackController returns 200 for
        // every authenticated branch, so the gateway will NOT resend -- this log line is the only
        // signal left.
        //
        // Same class as the "MONEY ARRIVED FOR A CLOSED ORDER" branch of OrderSettlementService, and
        // for the same reason: the system has no refund flow yet, so it must shout loudly for
        // someone to reconcile by hand.
        log.error(
            "TIỀN VỀ CHO LẦN THỬ ĐÃ XÓA SỔ -- cần đối soát thủ công: provider={} orderRef={} paymentId={} orderId={} trạngTháiĐangLưu={} amountVnd={}",
            input.provider(), verification.providerOrderRef(), payment.getId(),
            payment.getOrderId(), payment.getStatus(), payment.getAmountVnd());
        return PaymentCallbackResponse.toResponse(CallbackOutcome.PAID_AFTER_WRITE_OFF);
    }

    // Defence in layers: the signature already protects against payload tampering, but a mismatching amount means we
    // misunderstand the transaction -- stop instead of delivering goods against an order that has not been fully
    // collected. Compared with the amount of the ATTEMPT, not of the order: that is exactly the number sent to the gateway.
    private boolean isAmountMismatch(CallbackVerificationResult verification, PaymentRecord payment) {
        return verification.amount() != null
            && verification.amount().compareTo(payment.getAmountVnd()) != 0;
    }

    private PaymentCallbackResponse respondToAmountMismatch(
            ProcessPaymentCallbackCommand input, CallbackVerificationResult verification, PaymentRecord payment) {
        log.error("Callback {} lệch số tiền cho orderRef={}: nhận {} nhưng lần thử là {}",
            input.provider(), verification.providerOrderRef(), verification.amount(), payment.getAmountVnd());
        return PaymentCallbackResponse.toResponse(CallbackOutcome.AMOUNT_MISMATCH);
    }

    private PaymentCallbackResponse settleOrFailAttempt(
            ProcessPaymentCallbackCommand input, CallbackVerificationResult verification, PaymentRecord payment) {
        PaymentLinkRemoteStatus status = verification.status();
        if (status == PaymentLinkRemoteStatus.PAID) {
            log.info("[WEBHOOK] Chốt lần thanh toán {} (orderRef={}) thành PAID qua callback {}",
                payment.getId(), verification.providerOrderRef(), input.provider());
            orderSettlementService.settlePaid(payment, Instant.now());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.SETTLED);
        }
        // status null (the adapter could not conclude yet) or PENDING/PROCESSING/UNDERPAID: the transaction is not
        // fully done on the gateway side. Must NOT be failed here -- SePay sends intermediate notifications, and
        // closing an attempt that is still alive means the later "paid" notification has no row left to settle.
        PaymentStatus failureStatus = toFailureStatus(status);
        if (failureStatus == null) {
            log.info("Callback {} cho orderRef={} ở trạng thái {} -- chưa chốt gì",
                input.provider(), verification.providerOrderRef(), status);
            return PaymentCallbackResponse.toResponse(CallbackOutcome.NOT_FINAL);
        }
        // Only close the ATTEMPT, the order stays PENDING so the school can pay again -- see
        // OrderSettlementService.failAttempt.
        orderSettlementService.failAttempt(payment, failureStatus);
        return PaymentCallbackResponse.toResponse(CallbackOutcome.SETTLED);
    }

    private PaymentStatus toFailureStatus(PaymentLinkRemoteStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CANCELLED, EXPIRED, FAILED -> PaymentStatus.FAILED;
            default -> null;
        };
    }
}
