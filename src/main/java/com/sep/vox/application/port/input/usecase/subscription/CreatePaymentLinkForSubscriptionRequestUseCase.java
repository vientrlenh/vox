package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForSubscriptionRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.PaymentLinkResult;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class CreatePaymentLinkForSubscriptionRequestUseCase
        implements IUseCase<CreatePaymentLinkForSubscriptionRequestCommand, PaymentLinkDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PayOSPort payOSPort;
    private final UserContextPort userContextPort;
    // System Admin duyệt request qua PayOS phải quay về trang payment-result của System Admin (khác route/layout
    // với School Admin), nên cần return/cancel URL riêng. Rỗng nếu chưa cấu hình -> fallback về URL mặc định.
    private final String systemAdminReturnUrl;
    private final String systemAdminCancelUrl;

    public CreatePaymentLinkForSubscriptionRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            InvoiceRepository invoiceRepository,
            PayOSPort payOSPort,
            UserContextPort userContextPort,
            @Value("${payos.system-admin-return-url:}") String systemAdminReturnUrl,
            @Value("${payos.system-admin-cancel-url:}") String systemAdminCancelUrl) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.payOSPort = payOSPort;
        this.userContextPort = userContextPort;
        this.systemAdminReturnUrl = systemAdminReturnUrl;
        this.systemAdminCancelUrl = systemAdminCancelUrl;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(CreatePaymentLinkForSubscriptionRequestCommand input) {
        var request = subscriptionRequestRepository.findById(input.requestId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));

        if (!userContextPort.isSystemAdmin() && !request.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái chờ duyệt");
        }

        UUID existingSubscriptionId = request.getRequestType() == RequestType.UPGRADE
            ? schoolSubscriptionRepository.findActiveBySchoolId(request.getSchoolId())
                .map(subscription -> subscription.getId())
                .orElse(null)
            : null;

        var now = Instant.now();
        // Năm trong số hóa đơn phải lấy từ chính invoiceDate, không phải Year.now(): Year.now() đọc
        // múi giờ của JVM, nên trên server UTC một hóa đơn tạo lúc 06:00 ngày 01/01 giờ VN sẽ mang
        // số INV-2025-... nhưng ngày 2026-01-01.
        var invoiceDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        var invoiceNumber = "INV-" + invoiceDate.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            request.getSchoolId(),
            existingSubscriptionId,
            InvoiceSourceType.SUBSCRIPTION_REQUEST,
            request.getId(),
            invoiceDate,
            request.getAmount(),
            InvoiceStatus.PENDING,
            orderCode,
            null,
            null,
            null
        ));

        var useSystemAdminReturnUrls = userContextPort.isSystemAdmin()
            && !systemAdminReturnUrl.isBlank() && !systemAdminCancelUrl.isBlank();
        PaymentLinkResult result = useSystemAdminReturnUrls
            ? payOSPort.createPaymentLink(orderCode, request.getAmount(), "VOX-" + orderCode, systemAdminReturnUrl, systemAdminCancelUrl)
            : payOSPort.createPaymentLink(orderCode, request.getAmount(), "VOX-" + orderCode);

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.checkoutUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(savedInvoice.getId(), orderCode, result.paymentLinkId(), result.checkoutUrl());
    }
}