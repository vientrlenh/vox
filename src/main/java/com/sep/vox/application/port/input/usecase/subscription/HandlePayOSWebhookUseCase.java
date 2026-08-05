package com.sep.vox.application.port.input.usecase.subscription;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.domain.repository.InvoiceRepository;

// Internal service-to-service use case (webhook callback từ PayOS, xác thực bằng chữ ký thay vì JWT) —
@Service
public class HandlePayOSWebhookUseCase implements IUseCase<Object, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlePayOSWebhookUseCase.class);
    private static final String SUCCESS_CODE = "00";

    private final PaymentProcessPort paymentProcessPort;
    private final InvoiceRepository invoiceRepository;
    private final PayOSInvoiceSettlementService settlementService;

    public HandlePayOSWebhookUseCase(
            PaymentProcessResolver paymentProcessResolver,
            InvoiceRepository invoiceRepository,
            PayOSInvoiceSettlementService settlementService) {
        this.paymentProcessPort = paymentProcessResolver.resolve("payos");
        this.invoiceRepository = invoiceRepository;
        this.settlementService = settlementService;
    }

    @Override
    @Transactional
    public Void execute(Object rawWebhookBody) {
        if (!(rawWebhookBody instanceof Map<?, ?> rawBody)) {
            throw new IllegalArgumentException("Lỗi định dạng PayOS webhook");
        }
        var body = new HashMap<String, Object>(rawBody.size());
        for (var e : rawBody.entrySet()) {
            if (e.getKey() instanceof String k) {
                body.put(k, e.getValue());
            }
        }
        var dataObj = body.get("data");
        var signatureObj = (String) body.get("signature");
        if (!(dataObj instanceof Map<?, ?> rawData) || !(signatureObj instanceof String signature)) {
            throw new UnauthorizedException("Signature PayOS lỗi");
        }
        var data = new HashMap<String, Object>(rawData.size());
        for (var e : rawData.entrySet()) {
            if (e.getKey() instanceof String k) {
                data.put(k, e.getValue());
            }
        }
        if (!paymentProcessPort.verifyWebhookSignature(data, signature)) {
            throw new UnauthorizedException("Signature PayOS lỗi");
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