package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RenewSubscriptionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class CreatePaymentLinkForRenewalUseCase implements IUseCase<RenewSubscriptionCommand, PaymentLinkDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final PayOSPort payOSPort;
    private final UserContextPort userContextPort;

    public CreatePaymentLinkForRenewalUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            InvoiceRepository invoiceRepository,
            PayOSPort payOSPort,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.invoiceRepository = invoiceRepository;
        this.payOSPort = payOSPort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(RenewSubscriptionCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói đăng ký không ở trạng thái đang hoạt động");
        }

        var plan = subscriptionPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        var now = OffsetDateTime.now();
        var orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        var invoiceNumber = "INV-" + Year.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            subscription.getId(),
            InvoiceSourceType.SUBSCRIPTION,
            subscription.getId(),
            now.toLocalDate(),
            plan.getPricePerYear(),
            InvoiceStatus.PENDING,
            orderCode,
            null,
            null,
            null
        ));

        var result = payOSPort.createPaymentLink(orderCode, plan.getPricePerYear(), "VOX-" + orderCode);

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.checkoutUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(savedInvoice.getId(), orderCode, result.paymentLinkId(), result.checkoutUrl());
    }
}
