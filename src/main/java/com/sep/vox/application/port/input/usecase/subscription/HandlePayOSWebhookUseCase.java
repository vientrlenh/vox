package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.domain.repository.InvoiceRepository;

import vn.payos.exception.WebhookException;

// Internal service-to-service use case (webhook callback từ PayOS, xác thực bằng chữ ký thay vì JWT) —
// không dùng UserContextPort/ApproveRequestUseCase vì luồng này không có người dùng đăng nhập.
@Service
public class HandlePayOSWebhookUseCase implements IUseCase<Object, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlePayOSWebhookUseCase.class);
    private static final String SUCCESS_CODE = "00";

    private final PayOSPort payOSPort;
    private final InvoiceRepository invoiceRepository;
    private final PayOSInvoiceSettlementService settlementService;

    public HandlePayOSWebhookUseCase(
            PayOSPort payOSPort,
            InvoiceRepository invoiceRepository,
            PayOSInvoiceSettlementService settlementService) {
        this.payOSPort = payOSPort;
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

        if (data == null || signature == null || !payOSPort.verifyWebhookSignature(data, signature)) {
            throw new WebhookException("Invalid signature");
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