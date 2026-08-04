package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.service.PaymentPortResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PaymentPort;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.repository.InvoiceRepository;

// Internal service-to-service use case (webhook callback từ PayOS, xác thực bằng chữ ký thay vì JWT) —
@Service
public class HandlePayOSWebhookUseCase implements IUseCase<Object, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlePayOSWebhookUseCase.class);
    private static final String SUCCESS_CODE = "00";

    private final PaymentPort paymentPort;
    private final InvoiceRepository invoiceRepository;
    private final PayOSInvoiceSettlementService settlementService;

    public HandlePayOSWebhookUseCase(
            PaymentPortResolver paymentPortResolver,
            InvoiceRepository invoiceRepository,
            PayOSInvoiceSettlementService settlementService) {
        this.paymentPort = paymentPortResolver.resolve(PaymentMethod.PAYOS);
        this.invoiceRepository = invoiceRepository;
        this.settlementService = settlementService;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Void execute(Object rawWebhookBody) {
        var body = (Map<String, Object>) rawWebhookBody;
        var data = (Map<String, Object>) body.get("data");
        var signature = (String) body.get("signature");

        if (data == null || signature == null || !paymentPort.verifyWebhookSignature(data, signature)) {
            throw new UnauthorizedException("Invalid signature");
        }

        var orderCode = ((Number) data.get("orderCode")).longValue();
        var code = (String) data.get("code");

        var invoiceOpt = invoiceRepository.findByPayosOrderCode(orderCode);
        if (invoiceOpt.isEmpty()) {
            // Chữ ký hợp lệ nhưng không khớp đơn hàng nào (vd: payload test cố định của PayOS) —
            // vẫn trả 200 để tránh PayOS hiểu lầm webhook lỗi rồi retry liên tục.
            LOGGER.warn("Nhận webhook PayOS hợp lệ nhưng không tìm thấy invoice cho orderCode={}", orderCode);
            return null;
        }

        settlementService.settle(invoiceOpt.get(), SUCCESS_CODE.equals(code));
        return null;
    }
}