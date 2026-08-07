package com.sep.vox.infrastructure.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.repository.InvoiceRepository;

// Lưới an toàn cho các invoice PENDING mà cổng thanh toán không bao giờ gọi callback tới (vd: user tự
// hủy/đóng tab giữa chừng trên checkout UI, không có sự kiện server-to-server nào) — quét định kỳ và
// tự đối soát trạng thái qua API của cổng thay vì để hóa đơn treo PENDING mãi mãi.
//
// Adapter được resolve theo TỪNG hóa đơn chứ không cố định một cổng: job này phải phục vụ được mọi
// cổng đang bật, nếu không thì hóa đơn của cổng mới sẽ không bao giờ được đối soát.
@Component
public class PendingInvoiceReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingInvoiceReconciler.class);

    // SePay giới hạn Open API ở mức vài request/giây và trả 429 khi vượt; PayOS cũng không thích
    // bị dội. 400ms giữa hai lần hỏi (~2.5 req/s) nằm dưới ngưỡng của cả hai.
    private static final long THROTTLE_MILLIS = 400L;

    // Chặn trên số hóa đơn mỗi lượt để một lần tồn đọng bất thường không biến job thành cuộc gọi
    // API kéo dài hàng chục phút. Phần còn lại được xử lý ở lượt quét sau (mỗi 5 phút).
    private static final int MAX_INVOICES_PER_RUN = 200;

    private final InvoiceRepository invoiceRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final InvoiceSettlementService settlementService;

    public PendingInvoiceReconciler(
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentProcessResolver,
            InvoiceSettlementService settlementService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.settlementService = settlementService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        var pendingInvoices = invoiceRepository.findAllByStatus(InvoiceStatus.PENDING);
        var remoteCalls = 0;
        for (var invoice : pendingInvoices) {
            if (!isReconcilable(invoice)) {
                continue;
            }
            if (remoteCalls >= MAX_INVOICES_PER_RUN) {
                LOGGER.warn("Dừng đối soát ở {} hóa đơn trong lượt này, phần còn lại để lượt sau",
                    MAX_INVOICES_PER_RUN);
                return;
            }
            // Giãn nhịp TRƯỚC mỗi lời gọi (trừ lần đầu) thay vì sau: nếu vòng lặp thoát sớm vì lỗi
            // thì cũng không phải trả giá bằng một lần ngủ vô ích.
            if (remoteCalls > 0 && !throttle()) {
                return;
            }
            remoteCalls++;
            try {
                var paymentPort = paymentProcessResolver.resolve(invoice.getPaymentProvider());
                var remoteStatus = paymentPort.getPaymentLinkStatus(invoice.getProviderOrderRef()).status();
                if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                    LOGGER.info("[RECONCILER] Chốt hóa đơn {} (orderRef={}) thành PAID qua polling {}",
                        invoice.getId(), invoice.getProviderOrderRef(), invoice.getPaymentProvider());
                    settlementService.settle(invoice, true);
                } else {
                    var failureStatus = toFailureStatus(remoteStatus);
                    if (failureStatus != null) {
                        LOGGER.info("[RECONCILER] Chốt hóa đơn {} (orderRef={}) thành {} qua polling {}",
                            invoice.getId(), invoice.getProviderOrderRef(), failureStatus, invoice.getPaymentProvider());
                        settlementService.settle(invoice, false, failureStatus);
                    }
                }
            } catch (Exception e) {
                // Nuốt lỗi của từng hóa đơn để một cổng đang hỏng không chặn việc đối soát các hóa
                // đơn còn lại trong cùng lượt quét.
                LOGGER.warn("Lỗi khi đối soát hóa đơn PENDING provider={} orderRef={}",
                    invoice.getPaymentProvider(), invoice.getProviderOrderRef(), e);
            }
        }
    }

    // Trả false khi bị ngắt (vd ứng dụng đang tắt) để vòng lặp dừng hẳn thay vì chạy tiếp không
    // throttle và dội API của cổng.
    private boolean throttle() {
        try {
            Thread.sleep(THROTTLE_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Job đối soát bị ngắt giữa chừng, dừng lượt quét này");
            return false;
        }
    }

    // Hóa đơn thu ngoài hệ thống (MANUAL) không có cổng nào để hỏi, và về nguyên tắc cũng không bao
    // giờ ở trạng thái PENDING — bỏ qua thay vì để resolve() ném lỗi mỗi 5 phút.
    private boolean isReconcilable(Invoice invoice) {
        return invoice.getPaymentProvider() != null
            && invoice.getPaymentProvider().isOnlineGateway()
            && invoice.getProviderOrderRef() != null;
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
