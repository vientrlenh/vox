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
        CallbackVerificationResult verification = paymentProcessResolver
                .resolve(input.provider())
                .verifyCallback(input.rawBody(), input.headers());
        if (!verification.valid()) {
            throw new UnauthorizedException("Callback " + input.provider() + " không hợp lệ");
        }

        Optional<PaymentRecord> paymentOpt = paymentRecordRepository.findByProviderAndProviderOrderRef(
            input.provider(), verification.providerOrderRef());
        if (paymentOpt.isEmpty()) {
            log.warn("Nhận callback {} hợp lệ nhưng không tìm thấy lần thanh toán cho orderRef={}",
                input.provider(), verification.providerOrderRef());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.UNKNOWN_PAYMENT);
        }
        PaymentRecord payment = paymentOpt.get();

        // Callback lặp lại là chuyện BÌNH THƯỜNG, không phải lỗi: cổng gửi lại khi chưa nhận được
        // 200. Thoát sớm ở đây thay vì để settlePaid tự chặn, để phân biệt được "vừa chốt xong" và
        // "chốt từ lần gọi trước" trong log đối soát.
        if (payment.isSettled()) {
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
            if (verification.status() == PaymentLinkRemoteStatus.PAID
                    && payment.getStatus() != PaymentStatus.PAID) {
                log.error(
                    "TIỀN VỀ CHO LẦN THỬ ĐÃ XÓA SỔ -- cần đối soát thủ công: provider={} orderRef={} paymentId={} orderId={} trạngTháiĐangLưu={} amountVnd={}",
                    input.provider(), verification.providerOrderRef(), payment.getId(),
                    payment.getOrderId(), payment.getStatus(), payment.getAmountVnd());
                return PaymentCallbackResponse.toResponse(CallbackOutcome.PAID_AFTER_WRITE_OFF);
            }

            log.info("Callback {} lặp cho orderRef={} -- lần thử đã ở trạng thái {}",
                input.provider(), verification.providerOrderRef(), payment.getStatus());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.ALREADY_SETTLED);
        }

        // Phòng thủ nhiều lớp: chữ ký đã chống sửa payload, nhưng số tiền lệch nghĩa là mình đang
        // hiểu sai giao dịch -- dừng lại thay vì giao hàng theo một đơn chưa thu đủ. So với số tiền
        // của LẦN THỬ chứ không phải của đơn: đó chính là con số đã gửi sang cổng.
        if (verification.amount() != null
                && verification.amount().compareTo(payment.getAmountVnd()) != 0) {
            log.error("Callback {} lệch số tiền cho orderRef={}: nhận {} nhưng lần thử là {}",
                input.provider(), verification.providerOrderRef(), verification.amount(), payment.getAmountVnd());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.AMOUNT_MISMATCH);
        }

        PaymentLinkRemoteStatus status = verification.status();
        if (status == PaymentLinkRemoteStatus.PAID) {
            log.info("[WEBHOOK] Chốt lần thanh toán {} (orderRef={}) thành PAID qua callback {}",
                payment.getId(), verification.providerOrderRef(), input.provider());
            orderSettlementService.settlePaid(payment, Instant.now());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.SETTLED);
        }

        // status null (adapter chưa kết luận được) hoặc PENDING/PROCESSING/UNDERPAID: giao dịch chưa
        // xong hẳn bên cổng. KHÔNG được đánh hỏng ở đây -- SePay có gửi thông báo trung gian, và
        // đóng một lần thử còn sống nghĩa là lần báo "đã trả" sau đó không còn dòng nào để chốt.
        PaymentStatus failureStatus = toFailureStatus(status);
        if (failureStatus == null) {
            log.info("Callback {} cho orderRef={} ở trạng thái {} -- chưa chốt gì",
                input.provider(), verification.providerOrderRef(), status);
            return PaymentCallbackResponse.toResponse(CallbackOutcome.NOT_FINAL);
        }

        // Chỉ đóng LẦN THỬ, đơn vẫn PENDING để trường trả lại được -- xem
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
