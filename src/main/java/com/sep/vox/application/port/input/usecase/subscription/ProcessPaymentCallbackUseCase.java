package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.repository.InvoiceRepository;

/**
 * Xử lý callback đã xác thực của MỌI cổng thanh toán.
 *
 * <p>Thay cho cặp PayOSWebhookHandler/SePayIpnHandler trước đây: hai lớp đó có thân hàm giống hệt
 * nhau (verify -> tra hóa đơn -> đối chiếu tiền -> chốt), chỉ khác đúng hằng số PaymentMethod, và
 * được tra ra bằng tên bean dạng chuỗi nên gõ sai chỉ lộ lúc chạy. Ở đây cổng là tham số, và
 * PaymentProcessResolver lo phần chọn adapter.
 */
@Service
public class ProcessPaymentCallbackUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPaymentCallbackUseCase.class);

    private final PaymentProcessResolver paymentProcessResolver;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceSettlementService settlementService;

    public ProcessPaymentCallbackUseCase(
            PaymentProcessResolver paymentProcessResolver,
            InvoiceRepository invoiceRepository,
            InvoiceSettlementService settlementService) {
        this.paymentProcessResolver = paymentProcessResolver;
        this.invoiceRepository = invoiceRepository;
        this.settlementService = settlementService;
    }

    /**
     * Kết quả để tầng REST chọn mã HTTP trả về. Phân biệt quan trọng vì cổng thanh toán dùng chính
     * mã đó để quyết định có gọi lại hay không — SePay retry tới 7 lần trong ~5 giờ, nên chỉ những
     * trường hợp gọi lại mới có thể khác kết quả mới được trả lỗi.
     */
    public enum CallbackOutcome {
        /** Đã chốt hóa đơn (thành công hoặc thất bại). */
        SETTLED,
        /** Chữ ký/secret hợp lệ nhưng không khớp hóa đơn nào — vd payload test cố định của cổng. */
        UNKNOWN_ORDER,
        /** Số tiền báo về lệch với hóa đơn: dừng lại, cần người xem. */
        AMOUNT_MISMATCH,
        /** Trạng thái chưa phải trạng thái cuối (hoặc chưa ánh xạ được) — chưa chốt gì. */
        NOT_FINAL
    }

    @Transactional
    public CallbackOutcome execute(PaymentMethod provider, byte[] rawBody, Map<String, String> headers) {
        var verification = paymentProcessResolver.resolve(provider).verifyCallback(rawBody, headers);
        if (!verification.valid()) {
            throw new UnauthorizedException("Callback " + provider + " không hợp lệ");
        }

        var invoiceOpt = invoiceRepository.findByPaymentProviderAndProviderOrderRef(
            provider, verification.providerOrderRef());
        if (invoiceOpt.isEmpty()) {
            LOGGER.warn("Nhận callback {} hợp lệ nhưng không tìm thấy hóa đơn cho orderRef={}",
                provider, verification.providerOrderRef());
            return CallbackOutcome.UNKNOWN_ORDER;
        }
        var invoice = invoiceOpt.get();

        // Phòng thủ nhiều lớp: chữ ký đã chống sửa payload, nhưng số tiền lệch so với hóa đơn nghĩa
        // là ta đang hiểu sai giao dịch — dừng lại thay vì cấp quota theo một hóa đơn chưa thu đủ.
        if (verification.amount() != null && verification.amount().compareTo(invoice.getAmount()) != 0) {
            LOGGER.error("Callback {} lệch số tiền cho orderRef={}: nhận {} nhưng hóa đơn là {}",
                provider, verification.providerOrderRef(), verification.amount(), invoice.getAmount());
            return CallbackOutcome.AMOUNT_MISMATCH;
        }

        var status = verification.status();
        if (status == PaymentLinkRemoteStatus.PAID) {
            LOGGER.info("[WEBHOOK] Chốt hóa đơn {} (orderRef={}) thành PAID qua callback {}",
                invoice.getId(), verification.providerOrderRef(), provider);
            settlementService.settle(invoice, true);
            return CallbackOutcome.SETTLED;
        }

        // status null (adapter chưa kết luận được) hoặc PENDING/PROCESSING/UNDERPAID: giao dịch chưa
        // xong hẳn bên cổng. Không được chốt FAILED ở đây — SePay có thể gửi thông báo trung gian,
        // và đánh hỏng một hóa đơn đang chờ nghĩa là lần báo "đã trả" sau đó sẽ bị bỏ qua.
        var failureStatus = toFailureStatus(status);
        if (failureStatus == null) {
            LOGGER.info("Callback {} cho orderRef={} ở trạng thái {} — chưa chốt hóa đơn",
                provider, verification.providerOrderRef(), status);
            return CallbackOutcome.NOT_FINAL;
        }

        settlementService.settle(invoice, false, failureStatus);
        return CallbackOutcome.SETTLED;
    }

    private InvoiceStatus toFailureStatus(PaymentLinkRemoteStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CANCELLED -> InvoiceStatus.CANCELLED;
            case EXPIRED, FAILED -> InvoiceStatus.FAILED;
            default -> null;
        };
    }
}
