package com.sep.vox.infrastructure.callback;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.service.CallbackHandlerService;
import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.repository.InvoiceRepository;

@Component("payosCallback")
public class PayOSWebhookHandler implements CallbackHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayOSWebhookHandler.class);

    private final PaymentProcessPort paymentProcessPort;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceSettlementService settlementService;

    public PayOSWebhookHandler(
            PaymentProcessResolver paymentProcessResolver,
            InvoiceRepository invoiceRepository,
            InvoiceSettlementService settlementService) {
        this.paymentProcessPort = paymentProcessResolver.resolve(PaymentMethod.PAYOS);
        this.invoiceRepository = invoiceRepository;
        this.settlementService = settlementService;
    }

    @Override
    @Transactional
    public void handle(byte[] rawBody, Map<String, String> headers) {
        var verification = paymentProcessPort.verifyCallback(rawBody, headers);
        if (!verification.valid()) {
            throw new UnauthorizedException("Signature PayOS lỗi");
        }

        var invoiceOpt = invoiceRepository.findByPaymentProviderAndProviderOrderRef(
            PaymentMethod.PAYOS, verification.providerOrderRef());
        if (invoiceOpt.isEmpty()) {
            // Chữ ký hợp lệ nhưng không khớp đơn hàng nào (vd: payload test cố định của PayOS) —
            // vẫn trả 200 để tránh PayOS hiểu lầm webhook lỗi rồi retry liên tục.
            LOGGER.warn("Nhận webhook PayOS hợp lệ nhưng không tìm thấy invoice cho orderRef={}",
                verification.providerOrderRef());
            return;
        }
        var invoice = invoiceOpt.get();

        // Phòng thủ nhiều lớp: chữ ký đã chống sửa payload, nhưng số tiền lệch so với hóa đơn nghĩa
        // là ta đang hiểu sai giao dịch — dừng lại thay vì cấp quota theo một hóa đơn chưa thu đủ.
        if (verification.amount() != null && verification.amount().compareTo(invoice.getAmount()) != 0) {
            LOGGER.error("Webhook PayOS lệch số tiền cho orderRef={}: nhận {} nhưng hóa đơn là {}",
                verification.providerOrderRef(), verification.amount(), invoice.getAmount());
            return;
        }

        settlementService.settle(invoice, verification.status() == PaymentLinkRemoteStatus.PAID);
    }
}
