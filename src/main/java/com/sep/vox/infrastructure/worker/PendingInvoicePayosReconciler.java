package com.sep.vox.infrastructure.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.usecase.subscription.PayOSInvoiceSettlementService;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.repository.InvoiceRepository;

// Lưới an toàn cho các invoice PENDING mà webhook PayOS không bao giờ gọi tới (vd: user tự hủy/đóng tab
// giữa chừng trên checkout UI, không có sự kiện server-to-server nào từ PayOS) — quét định kỳ và tự đối
// soát trạng thái qua PayOS API thay vì chờ mãi mãi ở PENDING.
@Component
public class PendingInvoicePayosReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingInvoicePayosReconciler.class);

    private final InvoiceRepository invoiceRepository;
    private final PayOSPort payOSPort;
    private final PayOSInvoiceSettlementService settlementService;

    public PendingInvoicePayosReconciler(
            InvoiceRepository invoiceRepository,
            PayOSPort payOSPort,
            PayOSInvoiceSettlementService settlementService) {
        this.invoiceRepository = invoiceRepository;
        this.payOSPort = payOSPort;
        this.settlementService = settlementService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        var pendingInvoices = invoiceRepository.findAllByStatus(InvoiceStatus.PENDING);
        for (var invoice : pendingInvoices) {
            if (invoice.getPayosOrderCode() == null) {
                continue; // hóa đơn PENDING không qua PayOS (không nên xảy ra, phòng hờ)
            }
            try {
                var remoteStatus = payOSPort.getPaymentLinkStatus(invoice.getPayosOrderCode()).status();
                if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                    settlementService.settle(invoice, true);
                } else {
                    var failureStatus = toFailureStatus(remoteStatus);
                    if (failureStatus != null) {
                        settlementService.settle(invoice, false, failureStatus);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Lỗi khi đối soát hóa đơn PENDING orderCode={}", invoice.getPayosOrderCode(), e);
            }
        }
    }

    // null nghĩa là chưa phải trạng thái cuối (PENDING/PROCESSING/UNDERPAID) — không settle.
    private InvoiceStatus toFailureStatus(PaymentLinkRemoteStatus status) {
        return switch (status) {
            case CANCELLED -> InvoiceStatus.CANCELLED;
            case EXPIRED, FAILED -> InvoiceStatus.FAILED;
            default -> null;
        };
    }
}