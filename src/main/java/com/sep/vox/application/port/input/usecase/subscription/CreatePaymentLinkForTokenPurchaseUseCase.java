package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
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
    private final PaymentProcessResolver paymentProcessResolver;
    private final UserContextPort userContextPort;

    public CreatePaymentLinkForTokenPurchaseUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PlanQuotaRepository planQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentProcessResolver,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(BuyTokensCommand input) {
        var command = normalize(input);

        if (!userContextPort.isSystemAdmin() && !command.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var subscription = schoolSubscriptionRepository.findById(command.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói đăng ký không ở trạng thái đang hoạt động");
        }

        var planQuotas = planQuotaRepository.findAllByPlanId(subscription.getPlanId());
        var now = Instant.now();

        var total = BigDecimal.ZERO;
        for (var item : command.items()) {
            var planQuota = findPlanQuota(planQuotas, item.quotaType());
            total = total.add(planQuota.getTokenUnitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        // total_amount không cho update sau khi tạo (xem TokenPurchaseJpaEntity), nên phải tính total
        // trước rồi insert một lần với giá trị cuối cùng, không được insert 0 rồi set lại.
        var savedPurchase = tokenPurchaseRepository.save(new TokenPurchase(subscription.getId(), total, PurchaseStatus.PENDING, now));

        for (var item : command.items()) {
            var planQuota = findPlanQuota(planQuotas, item.quotaType());
            var subtotal = planQuota.getTokenUnitPrice().multiply(BigDecimal.valueOf(item.quantity()));

            tokenPurchaseItemRepository.save(new TokenPurchaseItem(
                savedPurchase.getId(), item.quotaType(), item.quantity(), planQuota.getTokenUnitPrice(), subtotal
            ));
        }

        var paymentMethod = PaymentMethod.resolveOnlineGateway(command.paymentMethod());
        var orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        // Năm trong số hóa đơn phải lấy từ chính invoiceDate, không phải Year.now(): Year.now() đọc
        // múi giờ của JVM, nên trên server UTC một hóa đơn tạo lúc 06:00 ngày 01/01 giờ VN sẽ mang
        // số INV-2025-... nhưng ngày 2026-01-01.
        var invoiceDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var invoiceNumber = "INV-" + invoiceDate.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            subscription.getSchoolId(),
            subscription.getId(),
            InvoiceSourceType.TOKEN_PURCHASE,
            savedPurchase.getId(),
            invoiceDate,
            total,
            InvoiceStatus.PENDING,
            paymentMethod,
            String.valueOf(orderCode),
            null,
            null,
            null,
            null
        ));

        var paymentProcessPort = paymentProcessResolver.resolve(paymentMethod);
        var result = paymentProcessPort.createPaymentLink(new PaymentProcessPort.CreatePaymentLinkCommand(
            String.valueOf(orderCode), total, "VOX-" + orderCode));

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.checkoutUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(savedInvoice.getId(), orderCode, result.paymentLinkId(), result.checkoutUrl());
    }

    private PlanQuota findPlanQuota(List<PlanQuota> planQuotas, QuotaType quotaType) {
        return planQuotas.stream()
            .filter(pq -> pq.getQuotaType() == quotaType)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn giá cho loại quota này"));
    }

    private BuyTokensCommand normalize(BuyTokensCommand input) {
        return new BuyTokensCommand(
            input.schoolId(), 
            input.subscriptionId(), 
            input.items(), 
            StringNormalization.normalizeCode(input.paymentMethod())
        );
    }

}