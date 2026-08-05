package com.sep.vox.infrastructure.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.repository.InvoiceRepository;

// Lưới an toàn cho các invoice PENDING mà webhook PayOS không bao giờ gọi tới (vd: user tự hủy/đóng tab
// giữa chừng trên checkout UI, không có sự kiện server-to-server nào từ PayOS) — quét định kỳ và tự đối
// soát trạng thái qua PayOS API thay vì chờ mãi mãi ở PENDING.
@Component
public class PendingInvoicePayosReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingInvoicePayosReconciler.class);

    private final InvoiceRepository invoiceRepository;
    private final PaymentProcessPort paymentPort;
    private final InvoiceSettlementService settlementService;

    public PendingInvoicePayosReconciler(
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentPortResolver,
            InvoiceSettlementService settlementService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentPort = paymentPortResolver.resolve(PaymentMethod.PAYOS);
        this.settlementService = settlementService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        var pendingInvoices = invoiceRepository.findAllByStatus(InvoiceStatus.PENDING);
        for (var invoice : pendingInvoices) {
            // Chỉ đối soát hóa đơn của đúng cổng mà job này cầm adapter. Không có guard này thì khi
            // SePay lên, job sẽ đem mã đơn SePay đi hỏi PayOS và log lỗi mỗi 5 phút mà không chốt
            // được gì. TODO(Phase 4): resolve adapter theo invoice.getPaymentProvider() để một job
            // đối soát được mọi cổng, thay vì bỏ qua như hiện tại.
            if (invoice.getPaymentProvider() != PaymentMethod.PAYOS || invoice.getProviderOrderRef() == null) {
                continue;
            }
            try {
                var remoteStatus = paymentPort.getPaymentLinkStatus(invoice.getProviderOrderRef()).status();
                if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                    settlementService.settle(invoice, true);
                } else {
                    var failureStatus = toFailureStatus(remoteStatus);
                    if (failureStatus != null) {
                        settlementService.settle(invoice, false, failureStatus);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Lỗi khi đối soát hóa đơn PENDING provider={} orderRef={}",
                    invoice.getPaymentProvider(), invoice.getProviderOrderRef(), e);
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