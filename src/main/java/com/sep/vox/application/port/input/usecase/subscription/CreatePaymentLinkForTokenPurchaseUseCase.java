package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

@Service
public class CreatePaymentLinkForTokenPurchaseUseCase implements IUseCase<BuyTokensCommand, PaymentLinkDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final PayOSPort payOSPort;
    private final UserContextPort userContextPort;

    public CreatePaymentLinkForTokenPurchaseUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PlanQuotaRepository planQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            InvoiceRepository invoiceRepository,
            PayOSPort payOSPort,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.payOSPort = payOSPort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(BuyTokensCommand input) {
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

        var planQuotas = planQuotaRepository.findAllByPlanId(subscription.getPlanId());
        var now = OffsetDateTime.now();
        var total = BigDecimal.ZERO;

        var purchase = tokenPurchaseRepository.save(new TokenPurchase(subscription.getId(), BigDecimal.ZERO, PurchaseStatus.PENDING, now));

        for (var item : input.items()) {
            var planQuota = planQuotas.stream()
                .filter(pq -> pq.getQuotaType() == item.quotaType())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn giá cho loại quota này"));
            var subtotal = planQuota.getTokenUnitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            total = total.add(subtotal);

            tokenPurchaseItemRepository.save(new TokenPurchaseItem(
                purchase.getId(), item.quotaType(), item.quantity(), planQuota.getTokenUnitPrice(), subtotal
            ));
        }

        purchase.setTotalAmount(total);
        var savedPurchase = tokenPurchaseRepository.save(purchase);

        var orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        var invoiceNumber = "INV-" + Year.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            subscription.getId(),
            InvoiceSourceType.TOKEN_PURCHASE,
            savedPurchase.getId(),
            now.toLocalDate(),
            total,
            InvoiceStatus.PENDING,
            orderCode,
            null,
            null,
            null
        ));

        var result = payOSPort.createPaymentLink(orderCode, total, "VOX-" + orderCode);

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.checkoutUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(savedInvoice.getId(), orderCode, result.paymentLinkId(), result.checkoutUrl());
    }
}