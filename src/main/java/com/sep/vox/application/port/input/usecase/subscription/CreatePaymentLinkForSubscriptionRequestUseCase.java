package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForSubscriptionRequestCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class CreatePaymentLinkForSubscriptionRequestUseCase
        implements IUseCase<CreatePaymentLinkForSubscriptionRequestCommand, PaymentLinkDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final UserContextPort userContextPort;

    public CreatePaymentLinkForSubscriptionRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentProcessResolver,
            UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(CreatePaymentLinkForSubscriptionRequestCommand input) {
        var command = normalize(input);
        var request = subscriptionRequestRepository.findById(command.requestId())
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
        
        var paymentMethod = PaymentMethod.resolveOnlineGateway(command.paymentMethod());
        var now = Instant.now();
        // Năm trong số hóa đơn phải lấy từ chính invoiceDate, không phải Year.now(): Year.now() đọc
        // múi giờ của JVM, nên trên server UTC một hóa đơn tạo lúc 06:00 ngày 01/01 giờ VN sẽ mang
        // số INV-2025-... nhưng ngày 2026-01-01.
        var invoiceDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var invoiceNumber = "INV-" + invoiceDate.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Mã đơn do adapter của cổng quyết định, không phải use case: PayOS đòi orderCode dạng số,
        // còn SePay dùng thẳng invoiceNumber dạng chuỗi.
        var paymentProcessPort = paymentProcessResolver.resolve(paymentMethod);
        var orderRef = paymentProcessPort.newOrderRef(invoiceNumber);

        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            request.getSchoolId(),
            existingSubscriptionId,
            InvoiceSourceType.SUBSCRIPTION_REQUEST,
            request.getId(),
            invoiceDate,
            request.getAmount(),
            InvoiceStatus.PENDING,
            paymentMethod,
            orderRef,
            null,
            null,
            null,
            null
        ));

        var result = paymentProcessPort.createPaymentLink(new CreatePaymentLinkCommand(
            orderRef, request.getAmount(), "VOX-" + orderRef));

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.actionUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(
            savedInvoice.getId(), orderRef, result.action(), result.actionUrl(), result.paymentLinkId(), result.fields());
    }

    private CreatePaymentLinkForSubscriptionRequestCommand normalize(CreatePaymentLinkForSubscriptionRequestCommand input) {
        return new CreatePaymentLinkForSubscriptionRequestCommand(
            input.requestId(), 
            StringNormalization.normalizeCode(input.paymentMethod())
        );
    }
}