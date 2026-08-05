package com.sep.vox.application.port.input.usecase.subscription;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.mapper.InvoiceDtoMapper;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.repository.InvoiceRepository;


@Service
public class SyncInvoicePaymentStatusUseCase implements IUseCase<UUID, InvoiceDto> {

    private final InvoiceRepository invoiceRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final InvoiceSettlementService settlementService;
    private final UserContextPort userContextPort;

    public SyncInvoicePaymentStatusUseCase(
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentProcessResolver,
            InvoiceSettlementService settlementService,
            UserContextPort userContextPort) {
        this.invoiceRepository = invoiceRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.settlementService = settlementService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public InvoiceDto execute(UUID invoiceId) {
        var invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn"));

        if (!userContextPort.isSystemAdmin() && !invoice.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        // Hóa đơn thu ngoài hệ thống (MANUAL) không có cổng nào để hỏi trạng thái — bỏ qua bước
        // đồng bộ thay vì để resolve() ném lỗi.
        if (invoice.getStatus() == InvoiceStatus.PENDING
                && invoice.getPaymentProvider() != null
                && invoice.getPaymentProvider().isOnlineGateway()
                && invoice.getProviderOrderRef() != null) {
            var paymentProcessPort = paymentProcessResolver.resolve(invoice.getPaymentProvider());
            var remoteStatus = paymentProcessPort.getPaymentLinkStatus(invoice.getProviderOrderRef()).status();
            if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                settlementService.settle(invoice, true);
            } else {
                var failureStatus = toFailureStatus(remoteStatus);
                if (failureStatus != null) {
                    settlementService.settle(invoice, false, failureStatus);
                }
            }
            // PENDING/PROCESSING/UNDERPAID: giao dịch chưa xong hẳn bên cổng, giữ nguyên PENDING.
        }

        var refreshed = invoiceRepository.findById(invoiceId)
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