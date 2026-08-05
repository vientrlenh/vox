package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.mapper.InvoiceDtoMapper;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.repository.InvoiceRepository;

// Đồng bộ trạng thái invoice theo yêu cầu (FE gọi khi PayOS redirect user về returnUrl/cancelUrl, vì
// PayOS không gọi webhook cho các trường hợp user tự hủy/không hoàn tất thanh toán trên checkout UI).
@Service
public class SyncInvoicePaymentStatusUseCase implements IUseCase<Long, InvoiceDto> {

    private final InvoiceRepository invoiceRepository;
    private final PaymentProcessPort paymentProcessPort;
    private final PayOSInvoiceSettlementService settlementService;
    private final UserContextPort userContextPort;

    public SyncInvoicePaymentStatusUseCase(
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentProcessResolver,
            PayOSInvoiceSettlementService settlementService,
            UserContextPort userContextPort) {
        this.invoiceRepository = invoiceRepository;
        this.paymentProcessPort = paymentProcessResolver.resolve("payos");
        this.settlementService = settlementService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public InvoiceDto execute(Long orderCode) {
        var invoice = invoiceRepository.findByPayosOrderCode(orderCode)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn"));

        if (!userContextPort.isSystemAdmin() && !invoice.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (invoice.getStatus() == InvoiceStatus.PENDING) {
            var remoteStatus = paymentProcessPort.getPaymentLinkStatus(orderCode).status();
            if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                settlementService.settle(invoice, true);
            } else {
                var failureStatus = toFailureStatus(remoteStatus);
                if (failureStatus != null) {
                    settlementService.settle(invoice, false, failureStatus);
                }
            }
            // PENDING/PROCESSING/UNDERPAID: giao dịch chưa xong hẳn bên PayOS, giữ nguyên PENDING.
        }

        var refreshed = invoiceRepository.findByPayosOrderCode(orderCode)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn"));
        return InvoiceDtoMapper.toDto(refreshed);
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