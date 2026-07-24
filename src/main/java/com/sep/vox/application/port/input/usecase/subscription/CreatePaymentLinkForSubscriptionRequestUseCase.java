package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForSubscriptionRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.application.port.output.UserContextPort;
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

    public CreatePaymentLinkForSubscriptionRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            InvoiceRepository invoiceRepository,
            PayOSPort payOSPort,
            UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.payOSPort = payOSPort;
        this.userContextPort = userContextPort;
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

        var now = OffsetDateTime.now();
        var orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        var invoiceNumber = "INV-" + Year.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            existingSubscriptionId,
            InvoiceSourceType.SUBSCRIPTION_REQUEST,
            request.getId(),
            now.toLocalDate(),
            request.getAmount(),
            InvoiceStatus.PENDING,
            orderCode,
            null,
            null,
            null
        ));

        var result = payOSPort.createPaymentLink(orderCode, request.getAmount(), "VOX-" + orderCode);

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.checkoutUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(savedInvoice.getId(), orderCode, result.paymentLinkId(), result.checkoutUrl());
    }
}