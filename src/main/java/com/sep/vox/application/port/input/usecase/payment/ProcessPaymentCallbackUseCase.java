package com.sep.vox.application.port.input.usecase.payment;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ProcessPaymentCallbackCommand;
import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.payment.PaymentCallbackResponse;
import com.sep.vox.application.response.input.payment.PaymentCallbackResponse.CallbackOutcome;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Xử lý callback đã xác thực của MỌI cổng thanh toán, và chốt kết quả cho lần thử tương ứng.
 *
 * <p>Điểm tra ngược giờ là {@code payment_records.provider_order_ref} chứ không phải hóa đơn: hóa
 * đơn chỉ tồn tại SAU khi tiền về nên lúc callback tới nó chưa có. provider_order_ref là thứ duy
 * nhất cổng gửi kèm mà mình đã ghi xuống TRƯỚC khi gọi sang cổng -- xem
 * CreatePaymentCheckoutUrlUseCase.
 *
 * <p>Cổng dùng mã HTTP để quyết định có gửi lại hay không, nên phân biệt các nhánh ở đây quyết định
 * hành vi retry của cổng. Xem CallbackController.
 */
@Service
public class ProcessPaymentCallbackUseCase implements IUseCase<ProcessPaymentCallbackCommand, PaymentCallbackResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPaymentCallbackUseCase.class);

    private final PaymentProcessResolver paymentProcessResolver;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OrderSettlementService orderSettlementService;

    public ProcessPaymentCallbackUseCase(
            PaymentProcessResolver paymentProcessResolver,
            PaymentRecordRepository paymentRecordRepository,
            OrderSettlementService orderSettlementService) {
        this.paymentProcessResolver = paymentProcessResolver;
        this.paymentRecordRepository = paymentRecordRepository;
        this.orderSettlementService = orderSettlementService;
    }

    @Transactional
    public PaymentCallbackResponse execute(ProcessPaymentCallbackCommand input) {
        var verification = paymentProcessResolver
                .resolve(input.provider())
                .verifyCallback(input.rawBody(), input.headers());
        if (!verification.valid()) {
            throw new UnauthorizedException("Callback " + input.provider() + " không hợp lệ");
        }

        var paymentOpt = paymentRecordRepository.findByProviderAndProviderOrderRef(
            input.provider(), verification.providerOrderRef());
        if (paymentOpt.isEmpty()) {
            LOGGER.warn("Nhận callback {} hợp lệ nhưng không tìm thấy lần thanh toán cho orderRef={}",
                input.provider(), verification.providerOrderRef());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.UNKNOWN_PAYMENT);
        }
        var payment = paymentOpt.get();

        // Callback lặp lại là chuyện BÌNH THƯỜNG, không phải lỗi: cổng gửi lại khi chưa nhận được
        // 200. Thoát sớm ở đây thay vì để settlePaid tự chặn, để phân biệt được "vừa chốt xong" và
        // "chốt từ lần gọi trước" trong log đối soát.
        if (payment.isSettled()) {
            // "Đã chốt" KHÔNG đồng nghĩa "vô hại". isSettled() chỉ là status != PENDING, nên một lần
            // thử đã bị ghi FAILED cũng rơi vào đây -- và cổng báo PAID cho đúng lần thử đó nghĩa là
            // TIỀN VỀ THẬT cho một lần thử mình đã xóa sổ.
            //
            // Đường vào phổ biến nhất là webhook tới KHÔNG ĐÚNG THỨ TỰ: cổng bắn EXPIRED trước, mình
            // ghi FAILED, rồi PAID mới tới. CallbackController trả 200 cho mọi nhánh đã xác thực nên
            // cổng sẽ KHÔNG gửi lại -- dòng log này là tín hiệu duy nhất còn lại.
            //
            // Cùng hạng với nhánh "TIỀN VỀ CHO ĐƠN ĐÃ ĐÓNG" của OrderSettlementService, và cũng cùng
            // lý do: hệ thống chưa có luồng hoàn tiền nào, nên phải kêu to để có người đối soát tay.
            if (verification.status() == PaymentLinkRemoteStatus.PAID
                    && payment.getStatus() != PaymentStatus.PAID) {
                LOGGER.error(
                    "TIỀN VỀ CHO LẦN THỬ ĐÃ XÓA SỔ -- cần đối soát thủ công: provider={} orderRef={} paymentId={} orderId={} trạngTháiĐangLưu={} amountVnd={}",
                    input.provider(), verification.providerOrderRef(), payment.getId(),
                    payment.getOrderId(), payment.getStatus(), payment.getAmountVnd());
                return PaymentCallbackResponse.toResponse(CallbackOutcome.PAID_AFTER_WRITE_OFF);
            }

            LOGGER.info("Callback {} lặp cho orderRef={} -- lần thử đã ở trạng thái {}",
                input.provider(), verification.providerOrderRef(), payment.getStatus());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.ALREADY_SETTLED);
        }

        // Phòng thủ nhiều lớp: chữ ký đã chống sửa payload, nhưng số tiền lệch nghĩa là mình đang
        // hiểu sai giao dịch -- dừng lại thay vì giao hàng theo một đơn chưa thu đủ. So với số tiền
        // của LẦN THỬ chứ không phải của đơn: đó chính là con số đã gửi sang cổng.
        if (verification.amount() != null
                && verification.amount().compareTo(payment.getAmountVnd()) != 0) {
            LOGGER.error("Callback {} lệch số tiền cho orderRef={}: nhận {} nhưng lần thử là {}",
                input.provider(), verification.providerOrderRef(), verification.amount(), payment.getAmountVnd());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.AMOUNT_MISMATCH);
        }

        var status = verification.status();
        if (status == PaymentLinkRemoteStatus.PAID) {
            LOGGER.info("[WEBHOOK] Chốt lần thanh toán {} (orderRef={}) thành PAID qua callback {}",
                payment.getId(), verification.providerOrderRef(), input.provider());
            orderSettlementService.settlePaid(payment, Instant.now());
            return PaymentCallbackResponse.toResponse(CallbackOutcome.SETTLED);
        }

        // status null (adapter chưa kết luận được) hoặc PENDING/PROCESSING/UNDERPAID: giao dịch chưa
        // xong hẳn bên cổng. KHÔNG được đánh hỏng ở đây -- SePay có gửi thông báo trung gian, và
        // đóng một lần thử còn sống nghĩa là lần báo "đã trả" sau đó không còn dòng nào để chốt.
        var failureStatus = toFailureStatus(status);
        if (failureStatus == null) {
            LOGGER.info("Callback {} cho orderRef={} ở trạng thái {} -- chưa chốt gì",
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
